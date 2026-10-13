package de.sprechbruecke;

import android.content.Context;
import android.graphics.Rect;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Runs on cover-sized and unfolded-sized windows, with system font scale 0.8, 1.0 and 2.0. */
@RunWith(AndroidJUnit4.class)
public class ResponsiveLayoutTest {
    @Test public void bothDesignsIgnoreSystemFontScaleAndKeepControlsReachable() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        for (String design : new String[]{"DIALOG", "NIGHT"}) {
            context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().clear()
                .putString("design", design).putString("mode", design.equals("DIALOG") ? "CONVERSATION" : "LIVE").commit();
            try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
                scenario.onActivity(a -> {
                    TextView input = a.findViewById(R.id.input_text);
                    TextView output = a.findViewById(R.id.output_text);
                    input.setText("Hallo, wie geht es dir?"); output.setText("Cześć, jak się masz?");
                });
                androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a -> {
                    View root = a.findViewById(R.id.screen);
                    TextView conversation = a.findViewById(R.id.mode_conversation);
                    assertEquals("Conversation label must not break inside the word", 1, conversation.getLineCount());
                    float density = a.getResources().getDisplayMetrics().density;
                    int width = a.getResources().getConfiguration().screenWidthDp;
                    float scale = a.getResources().getConfiguration().fontScale;
                    TextView output = a.findViewById(R.id.output_text);
                    assertEquals("Activity and dialogs use app font scale", 1f, scale, .001f);
                    float expected = 24 * density;
                    assertEquals("Translation size is fixed across system font settings", expected, output.getTextSize(), .5f);
                    assertEquals("Readable tab labels", 16 * density, conversation.getTextSize(), .5f);
                    android.widget.Button micButton = (android.widget.Button)a.findViewById(design.equals("DIALOG") ? R.id.german_mic : R.id.main_mic);
                    assertEquals("Microphone symbol is enlarged", Math.round(40 * density), micButton.getCompoundDrawables()[1].getBounds().width());
                    assertTrue("Text must not be vertically clipped", output.getLayout().getHeight() <= output.getHeight() - output.getCompoundPaddingTop() - output.getCompoundPaddingBottom());
                    assertTrue(a.findViewById(R.id.settings).getGlobalVisibleRect(new Rect()));
                    View mic = a.findViewById(design.equals("DIALOG") ? R.id.german_mic : R.id.main_mic);
                    assertTrue(mic.getWidth() >= 48 * density);
                    assertTrue(mic.getHeight() >= 48 * density);
                    assertEquals("App type keeps circular microphones", mic.getWidth(), mic.getHeight());
                    checkBounds(root, root.getWidth());
                });
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(design.equals("DIALOG") ? R.id.german_mic : R.id.main_mic))
                    .perform(androidx.test.espresso.action.ViewActions.scrollTo())
                    .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed()));
                androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withId(R.id.screen)).perform(androidx.test.espresso.action.ViewActions.swipeDown(), androidx.test.espresso.action.ViewActions.swipeDown());
                String suffix = context.getResources().getConfiguration().screenWidthDp + "-font-" + context.getResources().getConfiguration().fontScale;
                DesignSwitchTest.captureScreenshot("responsive-" + design + "-" + suffix);
            }
        }
    }
    private void checkBounds(View view, int windowWidth) {
        assertTrue("No control may be wider than the window", view.getWidth() <= windowWidth);
        if (view instanceof TextView && !(view instanceof android.widget.EditText)) {
            TextView text = (TextView) view;
            if (text.getLayout() != null && text.getEllipsize() == null)
                assertTrue("Text clipped: " + text.getText(), text.getLayout().getHeight() <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
        }
        if (view instanceof ViewGroup) for (int i=0; i<((ViewGroup)view).getChildCount(); i++) checkBounds(((ViewGroup)view).getChildAt(i), windowWidth);
    }
}
