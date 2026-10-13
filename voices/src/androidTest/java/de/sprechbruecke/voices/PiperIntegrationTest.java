package de.sprechbruecke.voices;

import android.content.Context;
import android.os.ParcelFileDescriptor;
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
            models.installArchive(voice,archive,()->false); assertTrue(models.installed(voice));
            OfflineTts tts=PiperEngine.create(models.directory(voice),voice);
            try {
                String phrase=voice.language.equals("pl") ? "Cześć! Jak się masz? Miło cię poznać." : "Hallo! Wie geht es dir? Schön, dich kennenzulernen.";
                GeneratedAudio audio=tts.generate(phrase,0,1f);
                assertEquals(22050,audio.getSampleRate()); assertTrue(audio.getSamples().length>22050);
                double energy=0; for(float sample:audio.getSamples()) { assertTrue(Float.isFinite(sample)); energy+=sample*sample; }
                assertTrue("Output must contain audible signal",energy/audio.getSamples().length>0.00001);
                File wav=new File(context.getFilesDir(),voice.id+".wav"); assertTrue(audio.save(wav.getAbsolutePath()));
                copyToReports(wav);
                int[] callbacks={0};
                tts.generateWithCallback(phrase+" "+phrase,0,1f,samples -> { callbacks[0]++; return 0; });
                assertEquals("Native streaming synthesis must respect cancellation",1,callbacks[0]);
                System.out.println("PIPER_OFFLINE_OK "+voice.name+" samples="+audio.getSamples().length);
            } finally { tts.release(); }
            // An interrupted reinstall must not damage the installed copy.
            try { models.installArchive(voice,archive,()->true); fail("Expected cancellation"); } catch(InterruptedIOException expected) { }
            assertTrue(models.installed(voice));
        }
    }
    private void copyToReports(File wav) throws Exception {
        for(String command:new String[]{"mkdir -p /data/local/tmp/sprechbruecke-audio", "cp "+wav.getAbsolutePath()+" /data/local/tmp/sprechbruecke-audio/"+wav.getName()}) {
            ParcelFileDescriptor fd=InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
            try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)) { byte[] b=new byte[4096]; while(in.read(b)!=-1){} }
        }
    }
}
