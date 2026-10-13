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
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class VoiceActivityTest {
    @Test public void launcherOpensWithoutModelsAndSurvivesRecreation() throws Exception {
        Intent launch=ApplicationProvider.getApplicationContext().getPackageManager().getLaunchIntentForPackage("de.sprechbruecke.voices");
        assertNotNull(launch);
        try(ActivityScenario<VoiceActivity> scenario=ActivityScenario.launch(launch)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            Thread.sleep(2000); // Include asynchronous TTS initialization on a fresh installation.
            scenario.onActivity(this::assertManager);
            scenario.recreate();
            scenario.onActivity(this::assertManager);
        }
    }
    @Test public void translatorInstallActionOpensManager() {
        Intent launch=new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA).setPackage("de.sprechbruecke.voices").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try(ActivityScenario<VoiceActivity> scenario=ActivityScenario.launch(launch)) { scenario.onActivity(this::assertManager); }
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
