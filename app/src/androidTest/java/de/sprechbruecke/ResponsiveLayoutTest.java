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

/** Runs on cover-sized and unfolded-sized windows, with system font scale 1.0 and 2.0. */
@RunWith(AndroidJUnit4.class)
public class ResponsiveLayoutTest {
    @Test public void bothDesignsRespectSystemTypeAndKeepControlsReachable() throws Exception {
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
                    float density = a.getResources().getDisplayMetrics().density;
                    int width = a.getResources().getConfiguration().screenWidthDp;
                    float scale = a.getResources().getConfiguration().fontScale;
                    TextView output = a.findViewById(R.id.output_text);
                    float factor = Math.max(.94f, Math.min(1.08f, width / 380f));
                    float expected = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 22 * factor, a.getResources().getDisplayMetrics());
                    assertEquals("System SP scaling must be retained", expected, output.getTextSize(), .5f);
                    assertTrue("Text must not be vertically clipped", output.getLayout().getHeight() <= output.getHeight() - output.getCompoundPaddingTop() - output.getCompoundPaddingBottom());
                    assertTrue(a.findViewById(R.id.settings).getGlobalVisibleRect(new Rect()));
                    View mic = a.findViewById(design.equals("DIALOG") ? R.id.german_mic : R.id.main_mic);
                    assertTrue(mic.getWidth() >= 48 * density);
                    assertTrue(mic.getHeight() >= 48 * density);
                    if (scale <= 1.1f) {
                        assertEquals("Normal type uses circular microphones", mic.getWidth(), mic.getHeight());
                        Rect visible = new Rect();
                        assertTrue("Primary controls visible without scrolling at normal font size", mic.getGlobalVisibleRect(visible));
                        assertEquals(mic.getHeight(), visible.height());
                    }
                    checkBounds(root, root.getWidth());
                });
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
