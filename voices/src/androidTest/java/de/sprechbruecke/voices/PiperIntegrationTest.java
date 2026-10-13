package de.sprechbruecke.voices;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.k2fsa.sherpa.onnx.*;
import java.io.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Host provisions the original compressed archives, then disables networking before this test. */
@RunWith(AndroidJUnit4.class)
public class PiperIntegrationTest {
    @Test public void installAndSynthesizeAllThreeVoicesOffline() throws Exception {
        // The normal UI suite does not provision these large files; the dedicated CI invocation does.
        Context context=ApplicationProvider.getApplicationContext();
        org.junit.Assume.assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("requirePiperModels")));
        assertTrue("Real model archives must be provisioned", new File(context.getCacheDir(),PiperVoice.GOSIA.directory()+".tar.bz2").isFile());
        PiperModels models=PiperModels.get(context);
        for(PiperVoice voice:PiperVoice.ALL) {
            File archive=new File(context.getCacheDir(),voice.directory()+".tar.bz2");
            android.util.Log.i("PiperTest","Install "+voice.id);
            models.installArchive(voice,archive,()->false); assertTrue(models.installed(voice));
            android.util.Log.i("PiperTest","Load "+voice.id);
            OfflineTts tts=PiperEngine.create(models.directory(voice),voice);
            try {
                String phrase=voice.language.equals("pl") ? "Cześć! Jak się masz? Miło cię poznać." : "Hallo! Wie geht es dir? Schön, dich kennenzulernen.";
                android.util.Log.i("PiperTest","Generate "+voice.id);
                GeneratedAudio audio=tts.generate(phrase,0,1f);
                assertEquals(22050,audio.getSampleRate()); assertTrue(audio.getSamples().length>22050);
                double energy=0; for(float sample:audio.getSamples()) { assertTrue(Float.isFinite(sample)); energy+=sample*sample; }
                assertTrue("Output must contain audible signal",energy/audio.getSamples().length>0.00001);
                File wav=new File(context.getFilesDir(),voice.id+".wav"); assertTrue(audio.save(wav.getAbsolutePath()));
                int[] callbacks={0};
                tts.generateWithCallback(phrase+" "+phrase,0,1f,new PiperCallback() { @Override public Integer invoke(float[] samples) { callbacks[0]++; return 0; } });
                assertEquals("Native streaming synthesis must respect cancellation",1,callbacks[0]);
                System.out.println("PIPER_OFFLINE_OK "+voice.name+" samples="+audio.getSamples().length);
            } finally { tts.release(); }
            // An interrupted reinstall must not damage the installed copy.
            try { models.installArchive(voice,archive,()->true); fail("Expected cancellation"); } catch(InterruptedIOException expected) { }
            assertTrue(models.installed(voice));
        }
        verifyAndroidService(context);
    }
    private void verifyAndroidService(Context context) throws Exception {
        CountDownLatch initialized=new CountDownLatch(1);
        AtomicInteger initStatus=new AtomicInteger(-99);
        TextToSpeech speech=new TextToSpeech(context, status -> { initStatus.set(status); initialized.countDown(); }, context.getPackageName());
        try {
            assertTrue("TTS service initialization",initialized.await(30,TimeUnit.SECONDS));
            assertEquals(TextToSpeech.SUCCESS,initStatus.get());
            for(PiperVoice selected:PiperVoice.ALL) {
                Voice actual=null;
                for(Voice candidate:speech.getVoices()) if(candidate.getName().equals(selected.id)) actual=candidate;
                assertNotNull("Installed voice exposed by Android: "+selected.id,actual);
                assertFalse(actual.isNetworkConnectionRequired());
                assertEquals(TextToSpeech.SUCCESS,speech.setVoice(actual));
                CountDownLatch finished=new CountDownLatch(1);
                AtomicInteger error=new AtomicInteger(0);
                speech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    public void onStart(String id) {}
                    public void onDone(String id) { finished.countDown(); }
                    public void onError(String id) { error.set(1); finished.countDown(); }
                });
                File wav=new File(context.getFilesDir(),selected.id+"-android.wav");
                String phrase=selected.language.equals("pl") ? "Dzień dobry! Jak się masz?" : "Guten Tag! Wie geht es dir?";
                assertEquals(TextToSpeech.SUCCESS,speech.synthesizeToFile(phrase,new android.os.Bundle(),wav,selected.id));
                assertTrue("Android synthesis completion: "+selected.id,finished.await(180,TimeUnit.SECONDS));
                assertEquals("Android synthesis error",0,error.get());
                assertTrue("Android produced PCM audio",wav.length()>44100);
                System.out.println("PIPER_ANDROID_SERVICE_OK "+selected.id);
            }
        } finally { speech.shutdown(); }
    }
}
