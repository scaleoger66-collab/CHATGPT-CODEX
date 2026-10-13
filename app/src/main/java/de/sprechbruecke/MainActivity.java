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
import android.view.WindowInsets;
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
    private Translator translator;
    private SpeechRecognizer recognizer;
    private TextToSpeech speech;
    private TextView status, output, history;
    private TextView voiceInfo;
    private SharedPreferences voiceSettings;
    private String activeEngine = "";
    private final OperationEpoch speechEngines = new OperationEpoch();
    private boolean refreshSpeechOnResume;
    private EditText input;
    private Button microphone, direction;
    private Switch live, readAloud;
    private boolean germanSource = true, ready, listening, running, destroyed, speechReady;
    private int generation;
    private final OperationEpoch operations = new OperationEpoch();
    private final OperationEpoch recordings = new OperationEpoch();
    private final OperationEpoch utterances = new OperationEpoch();
    private String lastTranslation = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        voiceSettings = getSharedPreferences("voices", MODE_PRIVATE);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(28, 36, 28, 28);
        scroll.setOnApplyWindowInsetsListener((v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            scroll.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });
        scroll.addView(layout); setContentView(scroll);
        TextView title = new TextView(this); title.setText("Sprechbrücke · Deutsch & Polski"); title.setTextSize(24); layout.addView(title);
        status = label(layout, "Sprachmodelle einmalig herunterladen, danach offline übersetzen.");
        direction = button(layout, "Deutsch → Polski", v -> {
            stop(); germanSource = !germanSource;
            direction.setText(germanSource ? "Deutsch → Polski" : "Polski → Deutsch");
            lastTranslation = ""; output.setText(""); configureTranslator(); updateVoiceInfo();
        });
        button(layout, "Sprachmodelle laden / prüfen", v -> { stop(); download(); });
        live = new Switch(this); live.setText("Live-Modus · fortlaufende Sprachabschnitte"); layout.addView(live);
        live.setOnCheckedChangeListener((v, checked) -> stop());
        readAloud = new Switch(this); readAloud.setText("Im Gesprächsmodus automatisch vorlesen"); readAloud.setChecked(true); layout.addView(readAloud);
        microphone = button(layout, "Mikrofon starten", v -> {
            if (running) { stop(); status.setText("Aufnahme beendet."); return; }
            stop();
            if (!ready) { status.setText("Bitte zuerst Sprachmodelle laden."); return; }
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1); return;
            }
            running = true; listen();
        });
        label(layout, "Textübersetzung");
        input = new EditText(this); input.setHint("Text eingeben … / Wpisz tekst …"); input.setMinLines(3); input.setGravity(48);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5000)}); layout.addView(input);
        button(layout, "Text übersetzen", v -> { stop(); translate(input.getText().toString().trim(), false); });
        output = label(layout, ""); output.setTextSize(22); output.setTextIsSelectable(true);
        button(layout, "Übersetzung vorlesen", v -> { stop(); speak(lastTranslation); });
        voiceInfo = label(layout, "Sprachausgabe wird geladen …");
        button(layout, "Stimme & Hörprobe", v -> showSpeechOptions());
        label(layout, "Gesprächsverlauf · nur für diese Sitzung"); history = label(layout, ""); history.setTextIsSelectable(true);
        button(layout, "Verlauf löschen", v -> { stop(); history.setText(""); input.setText(""); output.setText(""); lastTranslation = ""; status.setText("Verlauf gelöscht."); });
        label(layout, "Offline nach Sprachpaket-Download. Live übersetzt einzelne Sprachabschnitte und pausiert dabei die Aufnahme. Warte auf ‚Höre zu‘, bevor du weitersprichst. Zum Antworten die Sprachrichtung wechseln. Text: maximal 5.000 Zeichen. Übersetzungen können Fehler enthalten.");
        if (state != null) {
            germanSource = state.getBoolean("germanSource", true);
            direction.setText(germanSource ? "Deutsch → Polski" : "Polski → Deutsch");
            input.setText(state.getString("input", ""));
            lastTranslation = state.getString("translation", ""); output.setText(lastTranslation);
            history.setText(state.getString("history", ""));
            live.setChecked(state.getBoolean("live"));
            readAloud.setChecked(state.getBoolean("readAloud", true));
        }
        configureTranslator();
        initializeSpeech();
    }

    private String voiceKey() { return "voice:" + activeEngine + ":" + target().getLanguage(); }
    private void initializeSpeech() {
        final long epoch = speechEngines.advance();
        speechReady = false;
        if (speech != null) { speech.stop(); speech.shutdown(); }
        String requested = voiceSettings.getString("engine", "");
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
    private List<OfflineVoiceChoice> offlineVoices() {
        List<OfflineVoiceChoice> all = new ArrayList<>();
        if (speechReady && speech.getVoices() != null) for (Voice voice : speech.getVoices()) {
            Set<String> features = voice.getFeatures();
            all.add(new OfflineVoiceChoice(voice.getName(), voice.getLocale().getLanguage(),
                voice.getLocale().toLanguageTag(), voice.getQuality(), voice.isNetworkConnectionRequired(),
                features == null || !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)));
        }
        return OfflineVoiceChoice.available(all, target().getLanguage());
    }
    private String engineLabel() {
        if (speech != null) for (TextToSpeech.EngineInfo engine : speech.getEngines())
            if (engine.name.equals(activeEngine)) return engine.label;
        return activeEngine;
    }
    private void updateVoiceInfo() {
        if (voiceInfo == null) return;
        if (!speechReady) { voiceInfo.setText("Sprachausgabe nicht bereit. Unter ‚Stimme & Hörprobe‘ einen Anbieter wählen."); return; }
        OfflineVoiceChoice selected = OfflineVoiceChoice.select(offlineVoices(), voiceSettings.getString(voiceKey(), ""));
        voiceInfo.setText(engineLabel() + " · " + (germanSource ? "Polski" : "Deutsch") + "\n"
            + (selected == null ? "Keine passende Offline-Stimme ausgewählt oder installiert."
            : "Offline-Stimme: " + selected.name));
    }
    private void showSpeechOptions() {
        stop();
        new AlertDialog.Builder(this).setTitle("Sprachausgabe · " + (germanSource ? "Polski" : "Deutsch"))
            .setItems(new String[]{"Anbieter wählen (Samsung / Google …)", "Offline-Stimme auswählen", "Hörprobe abspielen", "Sprechtempo", "Sprachpakete installieren", "Google-Sprachausgabe im Play Store"}, (dialog, which) -> {
                if (which == 0) chooseEngine();
                if (which == 1) chooseVoice();
                if (which == 2) previewVoice();
                if (which == 3) chooseRate();
                if (which == 4) installVoiceData();
                if (which == 5) openGoogleSpeech();
            }).setNegativeButton("Schließen", null).show();
    }
    private void chooseEngine() {
        List<TextToSpeech.EngineInfo> engines = speech == null ? new ArrayList<>() : speech.getEngines();
        String[] names = new String[engines.size() + 1]; names[0] = "Android-Standard";
        for (int i = 0; i < engines.size(); i++) names[i + 1] = engines.get(i).label;
        new AlertDialog.Builder(this).setTitle("Anbieter auswählen").setItems(names, (dialog, which) -> {
            voiceSettings.edit().putString("engine", which == 0 ? "" : engines.get(which - 1).name).apply();
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
        new AlertDialog.Builder(this).setTitle("Offline-Stimme wählen & anhören").setItems(labels, (dialog, which) -> {
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
        new AlertDialog.Builder(this).setTitle("Sprechtempo").setItems(labels, (dialog, which) -> {
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

    private TextView label(LinearLayout parent, String text) {
        TextView view = new TextView(this); view.setText(text); view.setPadding(0, 18, 0, 18); parent.addView(view); return view;
    }
    private Button button(LinearLayout parent, String text, View.OnClickListener action) {
        Button view = new Button(this); view.setText(text); view.setOnClickListener(action); parent.addView(view); return view;
    }
    private String source() { return germanSource ? TranslateLanguage.GERMAN : TranslateLanguage.POLISH; }
    private Locale target() { return Locale.forLanguageTag(germanSource ? "pl-PL" : "de-DE"); }
    private void configureTranslator() {
        generation++; ready = false;
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
                if (de && pl) { ready = true; status.setText("Textübersetzung offline bereit."); }
            }).addOnFailureListener(e -> { if (valid(token)) status.setText("Sprachmodelle laden / prüfen antippen."); });
    }
    private void download() {
        final int token = generation;
        final long operation = operations.advance();
        status.setText("Sprachmodelle werden geprüft oder heruntergeladen …");
        translator.downloadModelIfNeeded(new DownloadConditions.Builder().build())
            .addOnSuccessListener(v -> { if (valid(token)) { ready = true; if (operations.accepts(operation)) status.setText("Textübersetzung offline bereit."); } })
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
            if (fromMicrophone && running && live.isChecked()) scheduleListen();
            else if (fromMicrophone && running) { stop(); if (readAloud.isChecked()) speak(result); }
        }).addOnFailureListener(e -> { if (valid(token) && operations.accepts(operation)) { stop(); status.setText("Übersetzung fehlgeschlagen. Sprachmodelle prüfen."); } });
    }
    private void listen() {
        if (!running || destroyed) return;
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
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() { if (current()) status.setText("Sprachabschnitt wird erkannt …"); }
            @Override public void onEvent(int t, Bundle b) {}
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, germanSource ? "de-DE" : "pl-PL");
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        listening = true; microphone.setText("Mikrofon stoppen"); status.setText("Mikrofon wird gestartet …");
        try { recognizer.startListening(intent); }
        catch (SecurityException e) { stop(); status.setText("Mikrofonberechtigung fehlt."); }
    }
    private void scheduleListen() { handler.postDelayed(() -> { if (running && !listening) listen(); }, 350); }
    private void stop() {
        running = false; listening = false; handler.removeCallbacksAndMessages(null);
        operations.advance(); recordings.advance(); utterances.advance();
        if (recognizer != null) { recognizer.cancel(); recognizer.destroy(); recognizer = null; }
        if (speech != null) speech.stop();
        if (microphone != null) microphone.setText("Mikrofon starten");
    }
    private void speak(String text) {
        if (text.isEmpty()) return;
        if (text.length() > TextToSpeech.getMaxSpeechInputLength()) {
            status.setText("Dieser Text ist zu lang zum Vorlesen. Bitte in kürzere Abschnitte aufteilen."); return;
        }
        if (!speechReady) { status.setText("Sprachausgabe noch nicht verfügbar."); return; }
        OfflineVoiceChoice selected = OfflineVoiceChoice.select(offlineVoices(), voiceSettings.getString(voiceKey(), ""));
        if (selected == null) { status.setText("Bitte unter ‚Stimme & Hörprobe‘ eine Offline-Stimme wählen oder Sprachpakete installieren."); return; }
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
        else if (live.isChecked()) scheduleListen(); else stop();
    }
    @Override public void onPartialResults(Bundle results) {
        if (!running) return;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) input.setText(matches.get(0));
    }
    @Override public void onError(int error) {
        listening = false; if (!running) return;
        if (live.isChecked() && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) { scheduleListen(); return; }
        stop();
        if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
            status.setText("Offline-Sprachpaket fehlt oder wird nicht unterstützt. Bitte Deutsch/Polnisch in den Android-Spracheinstellungen prüfen.");
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
        state.putBoolean("live", live.isChecked()); state.putBoolean("readAloud", readAloud.isChecked());
    }
    @Override protected void onPause() { super.onPause(); stop(); }
    @Override protected void onResume() {
        super.onResume();
        if (refreshSpeechOnResume) { refreshSpeechOnResume = false; initializeSpeech(); }
    }
    @Override protected void onDestroy() { destroyed = true; stop(); if (recognizer != null) recognizer.destroy(); if (speech != null) speech.shutdown(); if (translator != null) translator.close(); super.onDestroy(); }
}
