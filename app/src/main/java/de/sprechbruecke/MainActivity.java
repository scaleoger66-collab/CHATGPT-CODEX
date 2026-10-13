package de.sprechbruecke;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
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

/** Offline translation; microphone never falls back to a cloud recognizer. */
public class MainActivity extends Activity implements RecognitionListener {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Translator translator;
    private SpeechRecognizer recognizer;
    private TextToSpeech speech;
    private TextView status, output, history;
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
            lastTranslation = ""; output.setText(""); configureTranslator();
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
        button(layout, "Sprachausgabe installieren", v -> {
            try { startActivity(new Intent("com.android.settings.TTS_SETTINGS")); }
            catch (android.content.ActivityNotFoundException e) { status.setText("Bitte Android-Einstellungen → Sprache → Sprachausgabe öffnen."); }
        });
        label(layout, "Gesprächsverlauf · nur für diese Sitzung"); history = label(layout, ""); history.setTextIsSelectable(true);
        button(layout, "Verlauf löschen", v -> { stop(); history.setText(""); input.setText(""); output.setText(""); lastTranslation = ""; status.setText("Verlauf gelöscht."); });
        label(layout, "Offline nach Sprachpaket-Download. Live übersetzt einzelne Sprachabschnitte und pausiert dabei die Aufnahme. Warte auf ‚Höre zu‘, bevor du weitersprichst. Zum Antworten die Sprachrichtung wechseln. Text: maximal 5.000 Zeichen. Übersetzungen können Fehler enthalten.");
        speech = new TextToSpeech(this, result -> { speechReady = result == TextToSpeech.SUCCESS; });
        speech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String id) {}
            @Override public void onDone(String id) { runOnUiThread(() -> { if (!destroyed && id.equals(Long.toString(utterances.current()))) status.setText("Bereit."); }); }
            @Override public void onError(String id) { runOnUiThread(() -> { if (!destroyed && id.equals(Long.toString(utterances.current()))) status.setText("Sprachausgabe fehlgeschlagen. Offline-Stimme prüfen."); }); }
        });
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
        if (speech.setLanguage(target()) < 0) { status.setText("Bitte eine Stimme für die Zielsprache installieren."); return; }
        android.speech.tts.Voice voice = speech.getVoice();
        if (voice == null || voice.isNetworkConnectionRequired()) {
            voice = null;
            if (speech.getVoices() != null) for (android.speech.tts.Voice candidate : speech.getVoices()) {
                if (candidate.getLocale().getLanguage().equals(target().getLanguage()) && !candidate.isNetworkConnectionRequired()) { voice = candidate; break; }
            }
            if (voice == null) { status.setText("Keine Offline-Stimme installiert. Bitte in den Spracheinstellungen herunterladen."); return; }
        }
        String utterance = Long.toString(utterances.advance());
        if (speech.setVoice(voice) != TextToSpeech.SUCCESS || speech.speak(text, TextToSpeech.QUEUE_FLUSH, null, utterance) != TextToSpeech.SUCCESS)
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
    @Override protected void onDestroy() { destroyed = true; stop(); if (recognizer != null) recognizer.destroy(); if (speech != null) speech.shutdown(); if (translator != null) translator.close(); super.onDestroy(); }
}
