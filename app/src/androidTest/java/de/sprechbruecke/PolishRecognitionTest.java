package de.sprechbruecke;
import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.vosk.Model;
import org.vosk.Recognizer;
import java.io.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
@RunWith(AndroidJUnit4.class)
public class PolishRecognitionTest {
    @Test public void recognizesPolishAudioWithOriginalModelOffline() throws Exception {
        assumeTrue("Dedicated offline fixture job", "true".equals(InstrumentationRegistry.getArguments().getString("requirePolishModel")));
        Context context = ApplicationProvider.getApplicationContext();
        File zip = new File(context.getCacheDir(), "polish.zip");
        assertTrue(zip.isFile()); PolishModel.verify(zip);
        File stage = new File(context.getCacheDir(), "polish-test");
        PolishModel.erase(stage); PolishModel.extract(zip, stage, bytes -> {});
        try (Model model = new Model(new File(stage, PolishModel.NAME).getAbsolutePath())) {
            for (String voice : new String[]{"gosia", "darkman"}) {
                StringBuilder transcript = new StringBuilder();
                try (Recognizer recognizer = new Recognizer(model, 16000f);
                     InputStream in = new FileInputStream(new File(context.getCacheDir(), voice + ".pcm"))) {
                    byte[] audio = new byte[6400]; int n;
                    while ((n = in.read(audio)) != -1)
                        if (recognizer.acceptWaveForm(audio, n)) transcript.append(PolishRecognition.text(recognizer.getResult(), "text")).append(' ');
                    transcript.append(PolishRecognition.text(recognizer.getFinalResult(), "text"));
                }
                String text = transcript.toString(); android.util.Log.i("PolishTest", voice + ": " + text);
                assertTrue(voice + ": " + text, text.contains("jak") && text.contains("masz"));
            }
        } finally { PolishModel.erase(stage); }
    }
}
