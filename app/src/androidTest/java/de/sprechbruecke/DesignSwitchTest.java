package de.sprechbruecke;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DesignSwitchTest {
    @Before public void clearAppearance() {
        ApplicationProvider.<Context>getApplicationContext().getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().clear().commit();
    }
    private void choose(String item) {
        onView(withId(R.id.screen)).perform(swipeDown(), swipeDown());
        onView(withId(R.id.settings)).perform(click());
        onView(withText("Design & Bedienung")).perform(click());
        onView(withText(item)).perform(click());
        onView(withText("Übernehmen")).perform(click());
    }
    @Test public void settingsSwitchBehaviorAndPreserveTextAcrossRecreation() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(a -> ((EditText) a.findViewById(R.id.input_text)).setText("Hallo, wie geht es dir?"));
            choose("Design 2 · Nacht & Fokus\nDunkel, startet im Live-Modus");
            onView(withId(R.id.mode_live)).check(matches(isSelected()));
            onView(withId(R.id.german_mic)).check(doesNotExist());
            onView(withId(R.id.main_mic)).check(matches(withText("Mikrofon starten")));
            onView(withId(R.id.input_text)).check(matches(withText("Hallo, wie geht es dir?")));
            scenario.onActivity(a -> assertEquals(Color.parseColor("#0B1115"), ((ColorDrawable)a.findViewById(R.id.screen).getBackground()).getColor()));
            screenshot("design-2-night");
            scenario.recreate();
            onView(withId(R.id.mode_live)).check(matches(isSelected()));
            onView(withId(R.id.input_text)).check(matches(withText("Hallo, wie geht es dir?")));
            choose("Design 3 · Dialog & Nähe\nHell, zwei Sprechtasten");
            onView(withId(R.id.mode_conversation)).check(matches(isSelected()));
            onView(withId(R.id.main_mic)).check(doesNotExist());
            onView(withId(R.id.german_mic)).check(matches(withText("Deutsch\nsprechen")));
            onView(withId(R.id.polish_mic)).check(matches(withText("Mów\npo polsku")));
            onView(withId(R.id.input_text)).check(matches(withText("Hallo, wie geht es dir?")));
            screenshot("design-3-dialog");
        }
    }
    @Test public void textModeAndExplicitDesignPersistAfterFreshLaunch() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            choose("Design 2 · Nacht & Fokus\nDunkel, startet im Live-Modus");
            onView(withId(R.id.mode_text)).perform(click());
            onView(withText("Übersetzen")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withId(R.id.main_mic)).check(doesNotExist());
        }
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.mode_text)).check(matches(isSelected()));
            scenario.onActivity(a -> assertEquals("NIGHT", a.getSharedPreferences("appearance", Context.MODE_PRIVATE).getString("design", "")));
        }
    }
    @Test public void conversationButtonsSelectTheirOwnLanguageAndHistorySurvivesThemeChange() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            screenshot("dialog-before-scroll");
            onView(withId(R.id.screen)).perform(swipeUp());
            try { onView(withId(R.id.polish_mic)).perform(click()); }
            catch (RuntimeException e) { screenshot("dialog-scroll-failure"); throw e; }
            onView(withText("Polski · Ausgangstext")).check(matches(withText("Polski · Ausgangstext")));
            onView(withId(R.id.german_mic)).perform(click());
            onView(withText("Deutsch · Ausgangstext")).check(matches(withText("Deutsch · Ausgangstext")));
            scenario.onActivity(a -> ((TextView) a.findViewById(R.id.history_text)).setText("DE: Hallo\nPL: Cześć"));
            choose("Design 2 · Nacht & Fokus\nDunkel, startet im Live-Modus");
            onView(withId(R.id.history_text)).check(matches(withText("DE: Hallo\nPL: Cześć")));
        }
    }
    private void screenshot(String name) throws Exception { captureScreenshot(name); }
    static void captureScreenshot(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        File folder = new File(ApplicationProvider.<Context>getApplicationContext().getExternalFilesDir(null), "screenshots");
        if (!folder.exists() && !folder.mkdirs()) throw new IllegalStateException("Cannot create screenshots folder");
        try (FileOutputStream out = new FileOutputStream(new File(folder, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
        // Android's test runner uninstalls the app at the end, removing its scoped files.
        // Copy test-only screenshots to a shell-owned folder before that cleanup.
        String[] commands = {"mkdir -p /data/local/tmp/sprechbruecke-screenshots",
            "cp " + new File(folder, name + ".png").getAbsolutePath() + " /data/local/tmp/sprechbruecke-screenshots/" + name + ".png"};
        for (String command : commands) {
            android.os.ParcelFileDescriptor copy = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
            try (java.io.InputStream stream = new android.os.ParcelFileDescriptor.AutoCloseInputStream(copy)) {
                byte[] buffer = new byte[1024]; while (stream.read(buffer) != -1) { /* wait for command */ }
            }
        }
    }
}
