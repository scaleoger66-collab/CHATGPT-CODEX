package de.sprechbruecke;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/** Native model and microphone are owned by one worker; callbacks are fenced on the main thread. */
final class PolishRecognition {
    interface Listener { void ready(); void partial(String text); void result(String text); void error(); }
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicLong epoch = new AtomicLong();
    private Model model;
    void cancel() { epoch.incrementAndGet(); }
    void close() { cancel(); worker.execute(() -> { if (model != null) { model.close(); model = null; } }); worker.shutdown(); }
    private void post(long token, Runnable action) { main.post(() -> { if (epoch.get() == token) action.run(); }); }
    @SuppressLint("MissingPermission") // Activity checks RECORD_AUDIO before starting.
    void start(File path, Listener listener) {
        long token = epoch.incrementAndGet();
        worker.execute(() -> {
            AudioRecord mic = null;
            String result = null;
            try {
                if (epoch.get() != token) return;
                if (model == null) model = new Model(path.getAbsolutePath());
                if (epoch.get() != token) return;
                try (Recognizer recognizer = new Recognizer(model, 16000f)) {
                    int minimum = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    if (minimum < 0) throw new IllegalStateException("Audio format unavailable");
                    mic = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(minimum, 16000));
                    if (mic.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("Microphone unavailable");
                    mic.startRecording();
                    if (mic.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) throw new IllegalStateException("Recording unavailable");
                    post(token, listener::ready);
                    short[] audio = new short[3200];
                    long deadline = android.os.SystemClock.elapsedRealtime() + 30000;
                    String previous = "";
                    while (epoch.get() == token && android.os.SystemClock.elapsedRealtime() < deadline) {
                        int count = mic.read(audio, 0, audio.length, AudioRecord.READ_NON_BLOCKING);
                        if (count < 0) throw new IllegalStateException("Microphone read failed");
                        if (count == 0) { Thread.sleep(20); continue; }
                        if (recognizer.acceptWaveForm(audio, count)) {
                            String text = text(recognizer.getResult(), "text");
                            if (!text.isEmpty()) { result = text; break; }
                        } else {
                            String partial = text(recognizer.getPartialResult(), "partial");
                            if (!partial.equals(previous)) { previous = partial; post(token, () -> listener.partial(partial)); }
                        }
                    }
                    if (result == null) result = text(recognizer.getFinalResult(), "text");
                }
            } catch (Exception | LinkageError e) {
                android.util.Log.e("PolishRecognition", "Recognition failed", e);
                post(token, listener::error);
            } finally {
                if (mic != null) { try { mic.stop(); } catch (IllegalStateException ignored) {} mic.release(); }
            }
            // Release microphone before translation, automatic reading, or the next live segment.
            if (result != null) { String value = result; post(token, () -> listener.result(value)); }
        });
    }
    static String text(String json, String field) throws org.json.JSONException { return new JSONObject(json).optString(field, "").trim(); }
}
