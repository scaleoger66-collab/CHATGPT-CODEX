package de.sprechbruecke;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Displays the recognizer's reported RMS level, never a simulated listening animation. */
final class LevelView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float level;
    LevelView(Context context, int color) {
        super(context); paint.setColor(color); paint.setStrokeCap(Paint.Cap.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void setLevel(float rms) { level = Math.max(0, Math.min(1, (rms + 2) / 12)); invalidate(); }
    void reset() { level = 0; invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = 25;
        float gap = getWidth() / (count + 1f);
        paint.setStrokeWidth(Math.max(2, gap * 0.25f));
        for (int i = 0; i < count; i++) {
            float taper = 1 - Math.abs(i - 12) / 13f;
            float variation = (i % 3 == 0 ? 1 : i % 3 == 1 ? .65f : .4f);
            float half = 2 + level * getHeight() * .42f * taper * variation;
            canvas.drawLine(gap * (i + 1), getHeight() / 2f - half, gap * (i + 1), getHeight() / 2f + half, paint);
        }
    }
}
