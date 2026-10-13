package de.sprechbruecke;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.ContextThemeWrapper;
import android.graphics.Insets;
import android.text.InputFilter;
import android.widget.*;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import android.speech.tts.Voice;

/** Offline translation; microphone never falls back to a cloud recognizer. */
public class MainActivity extends Activity implements RecognitionListener {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final PolishRecognition polishRecognition = new PolishRecognition();
    private static final java.util.concurrent.ExecutorService modelDownloads = java.util.concurrent.Executors.newSingleThreadExecutor();
    private java.util.concurrent.atomic.AtomicBoolean downloadCancelled;
    private AlertDialog modelDialog;
    private Translator translator;
    private SpeechRecognizer recognizer;
    private TextToSpeech speech;
    private TextView status, output, history;
    private TextView voiceInfo;
    private SharedPreferences voiceSettings;
    private String activeEngine = "";
    private static final String PIPER_ENGINE = "de.sprechbruecke.voices";
    private boolean piperEnabled() { return voiceSettings.getBoolean("piperEnabled",true); }
    private PiperVoice selectedPiper(Locale language) { return PiperVoice.forLanguage(language.getLanguage(),voiceSettings.getString("piper:pl","gosia")); }
    private boolean piperInstalled() {
        Intent intent=new Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE).setPackage(PIPER_ENGINE);
        return !getPackageManager().queryIntentServices(intent,0).isEmpty();
    }
    private final OperationEpoch speechEngines = new OperationEpoch();
    private boolean refreshSpeechOnResume;
    private EditText input;
    private Switch readAloud;
    private SharedPreferences appearance;
    private Presentation presentation;
    private TranslatorScreen screen;
    private boolean pendingRecording;
    private boolean germanSource = true, ready, listening, running, destroyed, speechReady;
    private int generation;
    private final OperationEpoch operations = new OperationEpoch();
    private final OperationEpoch recordings = new OperationEpoch();
    private final OperationEpoch utterances = new OperationEpoch();
    private String lastTranslation = "";

    @Override protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(base);
        // Product requirement: app typography, including dialogs, ignores system font scaling.
        android.content.res.Configuration override = new android.content.res.Configuration();
        override.fontScale = 1f;
        applyOverrideConfiguration(override);
    }

    @Override public void onCreate(Bundle state) {
        appearance = getSharedPreferences("appearance", MODE_PRIVATE);
        presentation = new Presentation(appearance.getString("design", "DIALOG"), appearance.getString("mode", null));
        setTheme(presentation.dark() ? R.style.AppDark : R.style.AppLight);
        super.onCreate(state);
        voiceSettings = getSharedPreferences("voices", MODE_PRIVATE);
        if (state != null) germanSource = state.getBoolean("germanSource", true);
        buildScreen();
        if (state != null) {
            input.setText(state.getString("input", ""));
            lastTranslation = state.getString("translation", ""); output.setText(lastTranslation);
            history.setText(state.getString("history", ""));
        }
        configureTranslator();
        initializeSpeech();
    }

    private void buildScreen() {
        String savedInput = input == null ? "" : input.getText().toString();
        if (input != null) {
            android.view.inputmethod.InputMethodManager keyboard = (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (keyboard != null) keyboard.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
        String savedHistory = history == null ? "" : history.getText().toString();
        String savedStatus = status == null ? "Sprachmodelle werden geprüft …" : status.getText().toString();
        screen = new TranslatorScreen(this, presentation, germanSource, new TranslatorScreen.Actions() {
            public void settings() { showSettings(); }
            public void mode(Presentation.Mode mode) { selectMode(mode); }
            public void swap() { changeSource(!germanSource); }
            public void record(boolean german) { requestRecording(german); }
            public void translate() { stop(); MainActivity.this.translate(input.getText().toString().trim(), false); }
            public void speak() { stop(); MainActivity.this.speak(lastTranslation); }
            public void speakSource() { stop(); MainActivity.this.speak(input.getText().toString(), Locale.forLanguageTag(germanSource ? "de-DE" : "pl-PL")); }
            public void voices() { showSpeechOptions(); }
            public void clear() {
                stop(); history.setText(""); input.setText(""); output.setText(""); lastTranslation = "";
                status.setText("Verlauf gelöscht.");
            }
        });
        input = screen.input; output = screen.output; status = screen.status; history = screen.history;
        voiceInfo = screen.voiceInfo; readAloud = screen.readAloud; screen.setModelsReady(ready);
        input.setText(savedInput); output.setText(lastTranslation); history.setText(savedHistory); status.setText(savedStatus);
        readAloud.setChecked(appearance.getBoolean("readAloud", true));
        readAloud.setOnCheckedChangeListener((v, checked) -> appearance.edit().putBoolean("readAloud", checked).apply());
        updateVoiceInfo();
    }
    private AlertDialog.Builder dialog() {
        return new AlertDialog.Builder(new ContextThemeWrapper(this, presentation.dark() ? R.style.AppDark : R.style.AppLight));
    }
    private void showSettings() {
        stop(); status.setText("Aufnahme pausiert · Einstellungen");
        dialog().setTitle("Einstellungen").setItems(new String[]{"Design & Bedienung", "Stimme & Hörprobe", "Textübersetzung: Modelle laden / prüfen", "Mikrofon: Polnisch offline einrichten"}, (d, which) -> {
            if (which == 0) showDesignPicker();
            if (which == 1) showSpeechOptions();
            if (which == 2) download();
            if (which == 3) showPolishModel();
        }).setNegativeButton("Schließen", null).show();
    }
    private void showDesignPicker() {
        final int[] selected = {presentation.dark() ? 0 : 1};
        dialog().setTitle("Design & Bedienung")
            .setSingleChoiceItems(new String[]{"Design 2 · Nacht & Fokus\nDunkel, startet im Live-Modus", "Design 3 · Dialog & Nähe\nHell, zwei Sprechtasten"}, selected[0], (d, which) -> selected[0] = which)
            .setPositiveButton("Übernehmen", (d, which) -> selectDesign(selected[0] == 0 ? Presentation.Design.NIGHT : Presentation.Design.DIALOG))
            .setNegativeButton("Abbrechen", null).show();
    }
    void selectDesign(Presentation.Design design) {
        stop(); presentation.chooseDesign(design); savePresentation();
        setTheme(presentation.dark() ? R.style.AppDark : R.style.AppLight); buildScreen();
        status.setText(presentation.dark() ? "Nacht & Fokus · Live-Modus bereit. Mikrofon zum Starten antippen."
            : "Dialog & Nähe · Zum Sprechen Deutsch oder Polski wählen.");
    }
    void selectMode(Presentation.Mode mode) {
        if (presentation.mode == mode) return;
        stop(); presentation.mode = mode; savePresentation(); buildScreen();
        status.setText(mode == Presentation.Mode.TEXT ? "Text eingeben und übersetzen."
            : mode == Presentation.Mode.LIVE ? "Live-Modus bereit. Mikrofon zum Starten antippen." : "Gesprächsmodus bereit.");
    }
    private void savePresentation() {
        appearance.edit().putString("design", presentation.design.name()).putString("mode", presentation.mode.name()).apply();
    }
    private void changeSource(boolean german) {
        stop();
        if (germanSource == german) return;
        germanSource = german; screen.updateDirection(german); lastTranslation = ""; output.setText("");
        configureTranslator(); updateVoiceInfo();
    }
    private void requestRecording(boolean german) {
        if (running && germanSource == german) { stop(); status.setText("Aufnahme beendet."); return; }
        stop();
        boolean modelsWereReady = ready;
        changeSource(german);
        if (!german && !PolishModel.installed(getFilesDir())) { showPolishModel(); return; }
        if (!modelsWereReady) { status.setText("Bitte unter Einstellungen zuerst die Sprachmodelle laden."); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1); return;
        }
        pendingRecording = true;
        if (ready) beginRecording();
        else status.setText("Sprachrichtung wird vorbereitet …");
    }
    private void beginRecording() {
        if (!pendingRecording || destroyed) return;
        pendingRecording = false; running = true; listen();
    }

    private String voiceKey() { return "voice:" + activeEngine + ":" + target().getLanguage(); }
    private void initializeSpeech() {
        final long epoch = speechEngines.advance();
        speechReady = false;
        if (speech != null) { speech.stop(); speech.shutdown(); }
        String requested = piperEnabled() ? PIPER_ENGINE : voiceSettings.getString("engine", "");
        if(piperEnabled() && !piperInstalled()) {
            speech=null; activeEngine=PIPER_ENGINE; updateVoiceInfo(); return;
        }
        voiceInfo.setText("Sprachausgabe wird geladen …");
        // Post even if initialization fails synchronously: speech must be assigned first.
        TextToSpeech.OnInitListener init = result -> new Handler(Looper.getMainLooper()).post(() -> {
            if (destroyed || !speechEngines.accepts(epoch)) return;
            speechReady = result == TextToSpeech.SUCCESS;
            if (speechReady && !requested.isEmpty()) {
                boolean found = false;
                for (TextToSpeech.EngineInfo engine : speech.getEngines()) found |= requested.equals(engine.name);
                if (!found) speechReady = false;
            }
            activeEngine = requested.isEmpty() ? speech.getDefaultEngine() : requested;
            if (activeEngine == null) activeEngine = "";
            updateVoiceInfo();
        });
        speech = requested.isEmpty() ? new TextToSpeech(this, init) : new TextToSpeech(this, init, requested);
        speech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onDone(String id) { runOnUiThread(() -> { if (!destroyed && id.equals(Long.toString(utterances.current()))) status.setText("Bereit."); }); }
            @Override public void onError(String id) { runOnUiThread(() -> { if (!destroyed && id.equals(Long.toString(utterances.current()))) status.setText("Sprachausgabe fehlgeschlagen. Offline-Sprachpaket prüfen."); }); }
        });
    }
    private List<OfflineVoiceChoice> offlineVoices() { return offlineVoices(target()); }
    private List<OfflineVoiceChoice> offlineVoices(Locale language) {
        List<OfflineVoiceChoice> all = new ArrayList<>();
        if (speechReady && speech.getVoices() != null) for (Voice voice : speech.getVoices()) {
            Set<String> features = voice.getFeatures();
            all.add(new OfflineVoiceChoice(voice.getName(), voice.getLocale().getLanguage(),
                voice.getLocale().toLanguageTag(), voice.getQuality(), voice.isNetworkConnectionRequired(),
                features == null || !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)));
        }
        return OfflineVoiceChoice.available(all, language.getLanguage());
    }
    private String engineLabel() {
        if (speech != null) for (TextToSpeech.EngineInfo engine : speech.getEngines())
            if (engine.name.equals(activeEngine)) return engine.label;
        return activeEngine;
    }
    private void updateVoiceInfo() {
        if (voiceInfo == null) return;
        if(piperEnabled()) {
            PiperVoice selected=selectedPiper(target());
            boolean available=false;
            if(speechReady)for(OfflineVoiceChoice voice:offlineVoices())if(voice.name.equals(selected.id))available=true;
            voiceInfo.setText(selected.name+" · "+selected.languageName()+" · "+(available ? "offline bereit" : "Stimmen-Modul öffnen / herunterladen"));
            return;
        }
        if (!speechReady) { voiceInfo.setText("Sprachausgabe nicht bereit. Unter ‚Stimme & Hörprobe‘ einen Anbieter wählen."); return; }
        OfflineVoiceChoice selected = OfflineVoiceChoice.select(offlineVoices(), voiceSettings.getString(voiceKey(), ""));
        voiceInfo.setText(engineLabel() + " · " + (germanSource ? "Polski" : "Deutsch") + "\n"
            + (selected == null ? "Keine passende Offline-Stimme ausgewählt oder installiert."
            : "Offline-Stimme: " + selected.name));
    }
    private void showSpeechOptions() {
        stop();
        dialog().setTitle("Stimme & Hörprobe").setItems(new String[]{"Stimmen herunterladen / verwalten", "Polnische Stimme: "+selectedPiper(Locale.forLanguageTag("pl")).name,
            "Deutsche Stimme: Thorsten High", "Sprachausgabe: "+(piperEnabled()?"Piper offline":"Android"), "Hörprobe abspielen", "Sprechtempo", "Android-Stimmen verwalten"},(d,which)->{
            if(which==0)openPiperManager();
            if(which==1)choosePolishVoice();
            if(which==2) { usePiper(); speak("Hallo! Wie geht es dir? Schön, dich kennenzulernen.",Locale.forLanguageTag("de-DE")); }
            if(which==3)chooseSpeechSystem();
            if(which==4)previewVoice();
            if(which==5)chooseRate();
            if(which==6)showAndroidVoices();
        }).setNegativeButton("Schließen",null).show();
    }
    private void openPiperManager() {
        stop();
        if(!piperInstalled()) {
            dialog().setTitle("Stimmen-Modul installieren").setMessage("Installiere zusätzlich Sprechbruecke-Stimmen-v0.2.apk aus unserem Download-Paket. Darin kannst du Gosia, Darkman und Thorsten High herunterladen. Danach hier erneut öffnen.").setPositiveButton("OK",null).show(); return;
        }
        refreshSpeechOnResume=true;
        try { startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).setPackage(PIPER_ENGINE)); }
        catch(android.content.ActivityNotFoundException e){status.setText("Bitte die App Sprechbrücke Stimmen öffnen.");}
    }
    private void usePiper() {
        if(!piperEnabled()) { stop(); voiceSettings.edit().putBoolean("piperEnabled",true).apply(); initializeSpeech(); }
    }
    private void choosePolishVoice() {
        dialog().setTitle("Polnische Stimme").setSingleChoiceItems(new String[]{"Gosia", "Darkman"},selectedPiper(Locale.forLanguageTag("pl"))==PiperVoice.DARKMAN?1:0,(d,which)->{
            stop(); voiceSettings.edit().putString("piper:pl",which==0?"gosia":"darkman").apply();usePiper();updateVoiceInfo();d.dismiss();
            speak("Cześć! Jak się masz? Miło cię poznać.",Locale.forLanguageTag("pl-PL"));
        }).setNegativeButton("Abbrechen",null).show();
    }
    private void chooseSpeechSystem() {
        dialog().setTitle("Sprachausgabe").setSingleChoiceItems(new String[]{"Piper · Gosia / Darkman / Thorsten High", "Android · andere installierte Anbieter"},piperEnabled()?0:1,(d,which)->{
            stop();voiceSettings.edit().putBoolean("piperEnabled",which==0).apply();initializeSpeech();d.dismiss();
        }).setNegativeButton("Abbrechen",null).show();
    }
    private void showAndroidVoices() {
        dialog().setTitle("Android-Stimmen").setItems(new String[]{"Anbieter wählen", "Offline-Stimme auswählen", "Sprachpakete installieren", "Google-Sprachausgabe im Play Store"},(d,w)->{
            if(w==0)chooseEngine();if(w==1)chooseVoice();if(w==2)installVoiceData();if(w==3)openGoogleSpeech();
        }).setNegativeButton("Schließen",null).show();
    }
    private void chooseEngine() {
        List<TextToSpeech.EngineInfo> engines = speech == null ? new ArrayList<>() : speech.getEngines();
        String[] names = new String[engines.size() + 1]; names[0] = "Android-Standard";
        for (int i = 0; i < engines.size(); i++) names[i + 1] = engines.get(i).label;
        dialog().setTitle("Anbieter auswählen").setItems(names, (dialog, which) -> {
            voiceSettings.edit().putBoolean("piperEnabled",false).putString("engine", which == 0 ? "" : engines.get(which - 1).name).apply();
            initializeSpeech();
        }).setNegativeButton("Abbrechen", null).show();
    }
    private void chooseVoice() {
        if (!speechReady) { status.setText("Bitte zuerst einen verfügbaren Sprachausgabe-Anbieter wählen."); return; }
        List<OfflineVoiceChoice> voices = offlineVoices();
        if (voices.isEmpty()) { status.setText("Keine Offline-Stimme für die Zielsprache. Unter ‚Stimme & Hörprobe‘ Sprachpakete installieren."); return; }
        String[] labels = new String[voices.size() + 1];
        labels[0] = "Automatisch · höchste gemeldete Qualität";
        for (int i = 0; i < voices.size(); i++) labels[i + 1] = voices.get(i).name + " · " + voices.get(i).tag;
        dialog().setTitle("Offline-Stimme wählen & anhören").setItems(labels, (dialog, which) -> {
            voiceSettings.edit().putString(voiceKey(), which == 0 ? "" : voices.get(which - 1).name).apply();
            updateVoiceInfo(); previewVoice();
        }).setNegativeButton("Abbrechen", null).show();
    }
    private void previewVoice() {
        speak(germanSource ? "Cześć! Jak się masz? Miło cię poznać. Czy możemy porozmawiać po polsku?"
            : "Hallo! Wie geht es dir? Schön, dich kennenzulernen. Können wir auf Deutsch sprechen?");
    }
    private void chooseRate() {
        String[] labels = {"Langsamer · 85 %", "Normal · 100 %", "Schneller · 110 %"};
        float[] rates = {0.85f, 1f, 1.1f};
        dialog().setTitle("Sprechtempo").setItems(labels, (dialog, which) -> {
            voiceSettings.edit().putFloat("rate", rates[which]).apply(); previewVoice();
        }).setNegativeButton("Abbrechen", null).show();
    }
    private void installVoiceData() {
        refreshSpeechOnResume = true;
        Intent install = new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        if (!activeEngine.isEmpty()) install.setPackage(activeEngine);
        try { startActivity(install); }
        catch (android.content.ActivityNotFoundException e) {
            try { startActivity(new Intent("com.android.settings.TTS_SETTINGS")); }
            catch (android.content.ActivityNotFoundException missing) {
                refreshSpeechOnResume = false; status.setText("Bitte Android-Einstellungen → Sprache → Sprachausgabe öffnen.");
            }
        }
    }
    private void openGoogleSpeech() {
        refreshSpeechOnResume = true;
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.tts"))); }
        catch (android.content.ActivityNotFoundException e) { refreshSpeechOnResume = false; status.setText("Bitte Google-Sprachausgabe im Play Store suchen."); }
    }

    private String source() { return germanSource ? TranslateLanguage.GERMAN : TranslateLanguage.POLISH; }
    private Locale target() { return Locale.forLanguageTag(germanSource ? "pl-PL" : "de-DE"); }
    private void configureTranslator() {
        generation++; ready = false; screen.setModelsReady(false);
        if (translator != null) translator.close();
        translator = Translation.getClient(new TranslatorOptions.Builder().setSourceLanguage(source())
            .setTargetLanguage(germanSource ? TranslateLanguage.POLISH : TranslateLanguage.GERMAN).build());
        status.setText("Sprachmodelle laden / prüfen antippen. Bereits geladene Modelle funktionieren offline.");
        final int token = generation;
        com.google.mlkit.common.model.RemoteModelManager.getInstance()
            .getDownloadedModels(TranslateRemoteModel.class).addOnSuccessListener(models -> {
                if (!valid(token)) return;
                boolean de = false, pl = false;
                for (TranslateRemoteModel model : models) {
                    de |= TranslateLanguage.GERMAN.equals(model.getLanguage());
                    pl |= TranslateLanguage.POLISH.equals(model.getLanguage());
                }
                if (de && pl) { ready = true; screen.setModelsReady(true); status.setText("Textübersetzung offline bereit."); beginRecording(); }
                else { pendingRecording = false; status.setText("Sprachmodelle fehlen. Bitte unter Einstellungen herunterladen."); }
            }).addOnFailureListener(e -> { if (valid(token)) { pendingRecording = false; status.setText("Sprachmodelle unter Einstellungen laden / prüfen."); } });
    }
    private void download() {
        final int token = generation;
        final long operation = operations.advance();
        status.setText("Sprachmodelle werden geprüft oder heruntergeladen …");
        translator.downloadModelIfNeeded(new DownloadConditions.Builder().build())
            .addOnSuccessListener(v -> { if (valid(token)) { ready = true; screen.setModelsReady(true); if (operations.accepts(operation)) status.setText("Textübersetzung offline bereit."); } })
            .addOnFailureListener(e -> { if (valid(token) && operations.accepts(operation)) status.setText("Modelle fehlen oder Download fehlgeschlagen. Für die Ersteinrichtung Internet einschalten."); });
    }
    private boolean valid(int token) { return !destroyed && token == generation; }
    private void translate(String text, boolean fromMicrophone) {
        if (text.isEmpty()) return;
        if (!ready) { status.setText("Bitte zuerst Sprachmodelle laden / prüfen."); stop(); return; }
        final int token = generation;
        final long operation = operations.advance();
        status.setText("Übersetze offline …");
        translator.translate(text).addOnSuccessListener(result -> {
            if (!valid(token) || !operations.accepts(operation)) return;
            lastTranslation = result; output.setText(result);
            history.append((germanSource ? "DE: " : "PL: ") + text + "\n" + (germanSource ? "PL: " : "DE: ") + result + "\n\n");
            if (history.length() > 24000) history.setText(history.getText().subSequence(history.length() - 16000, history.length()));
            status.setText("Übersetzt.");
            if (fromMicrophone && running && presentation.isLive()) scheduleListen();
            else if (fromMicrophone && running) { stop(); if (readAloud.isChecked()) speak(result); }
        }).addOnFailureListener(e -> { if (valid(token) && operations.accepts(operation)) { stop(); status.setText("Übersetzung fehlgeschlagen. Sprachmodelle prüfen."); } });
    }
    private void listen() {
        if (!running || destroyed) return;
        if (!germanSource) { listenPolish(); return; }
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(this)) {
            stop(); status.setText("Dieses Gerät bietet keine Android-Offline-Spracherkennung. Textübersetzung bleibt nutzbar."); return;
        }
        if (speech != null) speech.stop();
        if (recognizer != null) { recognizer.destroy(); recognizer = null; }
        final long recording = recordings.advance();
        try { recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(this); }
        catch (UnsupportedOperationException e) { stop(); status.setText("Offline-Spracherkennung ist nicht verfügbar."); return; }
        recognizer.setRecognitionListener(new RecognitionListener() {
            private boolean current() { return !destroyed && recordings.accepts(recording); }
            @Override public void onResults(Bundle b) { if (current()) MainActivity.this.onResults(b); }
            @Override public void onPartialResults(Bundle b) { if (current()) MainActivity.this.onPartialResults(b); }
            @Override public void onError(int e) { if (current()) MainActivity.this.onError(e); }
            @Override public void onReadyForSpeech(Bundle b) { if (current()) status.setText("Höre zu …"); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) { if (current()) screen.level.setLevel(v); }
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() { if (current()) status.setText("Sprachabschnitt wird erkannt …"); }
            @Override public void onEvent(int t, Bundle b) {}
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, germanSource ? "de-DE" : "pl-PL");
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        listening = true; screen.setRecording(true); status.setText("Mikrofon wird gestartet …");
        try { recognizer.startListening(intent); }
        catch (SecurityException e) { stop(); status.setText("Mikrofonberechtigung fehlt."); }
    }
    private void listenPolish() {
        if (!PolishModel.installed(getFilesDir())) { stop(); showPolishModel(); return; }
        if (speech != null) speech.stop();
        listening = true; screen.setRecording(true); status.setText("Polnische Offline-Erkennung wird gestartet …");
        final long recording = recordings.advance();
        polishRecognition.start(PolishModel.directory(getFilesDir()), new PolishRecognition.Listener() {
            private boolean current() { return !destroyed && running && recordings.accepts(recording); }
            public void ready() { if (current()) status.setText("Höre zu · Polski · offline …"); }
            public void partial(String text) { if (current() && !text.isEmpty()) input.setText(text); }
            public void result(String text) {
                if (!current()) return;
                listening = false; recordings.advance();
                if (!text.isEmpty()) { input.setText(text); translate(text, true); }
                else if (presentation.isLive()) scheduleListen();
                else { stop(); status.setText("Keine Sprache erkannt. Bitte erneut sprechen."); }
            }
            public void error() { if (current()) { stop(); status.setText("Polnische Mikrofon-Erkennung konnte nicht starten. Mikrofonzugriff und freien Speicher prüfen; App bei Bedarf neu starten."); } }
        });
    }
    private void showPolishModel() {
        stop();
        if (PolishModel.installed(getFilesDir())) {
            dialog().setTitle("Polnisches Mikrofon bereit")
                .setMessage("Vosk Polnisch ist installiert und funktioniert offline. Zum Sprechen ‚Mów po polsku‘ wählen. Gosia und Darkman sind unabhängig davon die Stimmen zum Vorlesen.")
                .setPositiveButton("OK", null).show(); return;
        }
        dialog().setTitle("Polnisch verstehen · offline")
            .setMessage("Für das Mikrofon wird ein eigenes Erkennungsmodell benötigt: Vosk Polnisch, etwa 50 MB Download. Bitte etwa 250 MB freien Speicher bereithalten. Nach dem Download funktioniert es ohne Internet.\n\nGosia, Darkman und Thorsten sind Vorlesestimmen und bleiben installiert.\n\nModell: Alpha Cephei · vosk-model-small-pl-0.22 · Apache 2.0. Während des Downloads die App geöffnet lassen.")
            .setPositiveButton("Herunterladen", (d, w) -> downloadPolishModel()).setNegativeButton("Später", null).show();
    }
    private void cancelModelDownload() {
        if (downloadCancelled != null) downloadCancelled.set(true);
        if (modelDialog != null) { modelDialog.dismiss(); modelDialog = null; }
    }
    private void downloadPolishModel() {
        cancelModelDownload();
        final java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean();
        downloadCancelled = cancelled;
        modelDialog = dialog().setTitle("Polnische Mikrofon-Erkennung")
            .setMessage("Download startet …").setNegativeButton("Abbrechen", (d, w) -> cancelModelDownload()).create();
        modelDialog.setCanceledOnTouchOutside(false);
        modelDialog.setOnCancelListener(d -> cancelModelDownload()); modelDialog.show();
        final java.io.File files = getFilesDir(), cache = getCacheDir();
        modelDownloads.execute(() -> {
            final long[] last = {0};
            try {
                if (cancelled.get()) return;
                PolishModel.install(files, cache, bytes -> {
                    if (cancelled.get()) throw new java.io.InterruptedIOException("Abgebrochen");
                    long now = android.os.SystemClock.elapsedRealtime();
                    if (now - last[0] > 300) {
                        last[0] = now;
                        handler.post(() -> { if (!destroyed && !cancelled.get() && modelDialog != null)
                            modelDialog.setMessage((bytes / 1000000) + " MB geladen · anschließend wird geprüft und entpackt …"); });
                    }
                });
                handler.post(() -> {
                    if (destroyed || cancelled.get()) return;
                    modelDialog.dismiss(); modelDialog = null;
                    status.setText("Polnische Mikrofon-Erkennung offline bereit. Jetzt ‚Mów po polsku‘ antippen.");
                });
            } catch (Exception e) {
                android.util.Log.w("PolishModel", "Model installation failed", e);
                handler.post(() -> {
                    if (destroyed || cancelled.get()) return;
                    modelDialog.dismiss(); modelDialog = null;
                    status.setText("Download fehlgeschlagen. Internet und freien Speicher prüfen und erneut versuchen.");
                });
            }
        });
    }
    private void scheduleListen() { handler.postDelayed(() -> { if (running && !listening) listen(); }, 350); }
    private void stop() {
        running = false; listening = false; pendingRecording = false; handler.removeCallbacksAndMessages(null);
        operations.advance(); recordings.advance(); utterances.advance();
        polishRecognition.cancel();
        if (recognizer != null) { recognizer.cancel(); recognizer.destroy(); recognizer = null; }
        if (speech != null) speech.stop();
        if (screen != null) screen.setRecording(false);
    }
    private void speak(String text) { speak(text, target()); }
    private void speak(String text, Locale language) {
        if (text.isEmpty()) return;
        if (text.length() > TextToSpeech.getMaxSpeechInputLength()) {
            status.setText("Dieser Text ist zu lang zum Vorlesen. Bitte in kürzere Abschnitte aufteilen."); return;
        }
        if (!speechReady) { status.setText(piperEnabled() ? "Stimmen-Modul öffnen / installieren: Stimme & Hörprobe → Stimmen herunterladen / verwalten." : "Sprachausgabe noch nicht verfügbar."); return; }
        String wanted=piperEnabled() ? selectedPiper(language).id : voiceSettings.getString("voice:" + activeEngine + ":" + language.getLanguage(), "");
        OfflineVoiceChoice selected = OfflineVoiceChoice.select(offlineVoices(language), wanted);
        if (selected == null) { status.setText(piperEnabled() ? selectedPiper(language).name+" fehlt. Unter Stimme & Hörprobe → Stimmen herunterladen / verwalten laden." : "Bitte unter ‚Stimme & Hörprobe‘ eine Offline-Stimme wählen oder Sprachpakete installieren."); return; }
        Voice voice = null;
        Set<Voice> available = speech.getVoices();
        if (available != null) for (Voice candidate : available) if (candidate.getName().equals(selected.name)) { voice = candidate; break; }
        if (voice == null || speech.setVoice(voice) != TextToSpeech.SUCCESS) { status.setText("Stimme nicht verfügbar. Bitte neu auswählen."); return; }
        speech.setSpeechRate(voiceSettings.getFloat("rate", 1f));
        String utterance = Long.toString(utterances.advance());
        status.setText("Lese vor …");
        if (speech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utterance) != TextToSpeech.SUCCESS)
            status.setText("Sprachausgabe konnte nicht gestartet werden.");
    }
    @Override public void onResults(Bundle results) {
        listening = false; if (!running) return;
        recordings.advance();
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) { input.setText(matches.get(0)); translate(matches.get(0), true); }
        else if (presentation.isLive()) scheduleListen(); else stop();
    }
    @Override public void onPartialResults(Bundle results) {
        if (!running) return;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) input.setText(matches.get(0));
    }
    @Override public void onError(int error) {
        listening = false; if (!running) return;
        if (presentation.isLive() && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) { scheduleListen(); return; }
        stop();
        if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
            status.setText("Die deutsche Offline-Mikrofon-Erkennung fehlt oder wird von Android nicht unterstützt. Ein Vorlese-Stimmenpaket ersetzt sie nicht.");
        else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
            status.setText("Bitte Mikrofonzugriff in den App-Einstellungen erlauben.");
        else status.setText("Spracherkennung beendet (" + error + "). Bitte erneut starten.");
    }
    @Override public void onReadyForSpeech(Bundle params) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float value) {}
    @Override public void onBufferReceived(byte[] buffer) {}
    @Override public void onEndOfSpeech() {}
    @Override public void onEvent(int type, Bundle params) {}
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 1) status.setText(results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED
            ? "Mikrofon erlaubt. Zum Aufnehmen erneut starten."
            : "Ohne Mikrofonberechtigung bleibt die Texteingabe verfügbar.");
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("germanSource", germanSource);
        state.putString("input", input.getText().toString());
        state.putString("translation", lastTranslation); state.putString("history", history.getText().toString());

    }
    @Override protected void onPause() { super.onPause(); stop(); cancelModelDownload(); }
    @Override protected void onResume() {
        super.onResume();
        if (refreshSpeechOnResume || (piperEnabled() && !speechReady && piperInstalled())) { refreshSpeechOnResume = false; initializeSpeech(); }
    }
    @Override protected void onDestroy() { destroyed = true; stop(); polishRecognition.close(); if (recognizer != null) recognizer.destroy(); if (speech != null) speech.shutdown(); if (translator != null) translator.close(); super.onDestroy(); }
}
