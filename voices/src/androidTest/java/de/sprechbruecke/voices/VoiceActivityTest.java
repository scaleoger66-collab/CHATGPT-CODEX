package de.sprechbruecke.voices;

import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class VoiceActivityTest {
    private Locale original;
    @Before public void useGermanSystemLocale() { original=Locale.getDefault(); Locale.setDefault(Locale.GERMANY); }
    @After public void restoreLocale() { Locale.setDefault(original); }
    @Test public void launcherOpensWithoutModelsAndSurvivesRecreation() throws Exception {
        Intent launch=ApplicationProvider.getApplicationContext().getPackageManager().getLaunchIntentForPackage("de.sprechbruecke.voices");
        assertNotNull(launch);
        try(ActivityScenario<VoiceActivity> scenario=ActivityScenario.launch(launch)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertSpeechServiceStarts(Locale.GERMANY);
            scenario.onActivity(this::assertManager);
            scenario.recreate();
            scenario.onActivity(this::assertManager);
        }
    }
    @Test public void translatorInstallActionOpensManager() throws Exception {
        Locale.setDefault(Locale.forLanguageTag("pl-PL"));
        Intent launch=new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).setPackage("de.sprechbruecke.voices").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try(ActivityScenario<VoiceActivity> scenario=ActivityScenario.launch(launch)) { assertSpeechServiceStarts(Locale.forLanguageTag("pl-PL")); scenario.onActivity(this::assertManager); }
    }
    private void assertSpeechServiceStarts(Locale language) throws Exception {
        assertEquals(language.getLanguage(),Locale.getDefault().getLanguage());
        CountDownLatch initialized=new CountDownLatch(1);
        AtomicInteger result=new AtomicInteger(-99);
        TextToSpeech speech=new TextToSpeech(ApplicationProvider.getApplicationContext(),status->{result.set(status);initialized.countDown();},"de.sprechbruecke.voices");
        try {
            assertTrue("Voice service must finish initialization",initialized.await(20,TimeUnit.SECONDS));
            assertEquals(TextToSpeech.SUCCESS,result.get());
            int available=speech.isLanguageAvailable(language);
            assertTrue("Language callback must work even before downloading a model",available==TextToSpeech.LANG_MISSING_DATA || available>=TextToSpeech.LANG_AVAILABLE);
        } finally { speech.shutdown(); }
    }
    private void assertManager(VoiceActivity activity) {
        View root=activity.getWindow().getDecorView();
        assertTrue(hasText(root,"Deine Offline-Stimmen"));
        assertTrue(hasText(root,"Gosia ·"));
        assertTrue(hasText(root,"Darkman ·"));
        assertTrue(hasText(root,"Thorsten High ·"));
        assertTrue(hasText(root,"Alle drei Stimmen herunterladen"));
    }
    private boolean hasText(View view,String text) {
        if(view instanceof TextView && ((TextView)view).getText().toString().contains(text))return true;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) if(hasText(((ViewGroup)view).getChildAt(i),text))return true;
        return false;
    }
}
