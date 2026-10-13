package de.sprechbruecke;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.*;

/** Density-normalized app typography; system font scaling is deliberately ignored. */
final class TranslatorScreen {
    interface Actions {
        void settings(); void mode(Presentation.Mode mode); void swap(); void record(boolean german);
        void translate(); void speak(); void speakSource(); void voices(); void clear();
    }
    final EditText input;
    final TextView output, status, history, voiceInfo;
    final Switch readAloud;
    final LevelView level;
    private final Activity activity;
    private final Presentation presentation;
    private final int background, surface, foreground, muted, accent, border;
    private final TextView sourceLabel, targetLabel, from, to, modelBadge;
    private final LinearLayout sourceCard, targetCard;
    private final ImageButton play, sourcePlay;
    private final Button microphone, germanMic, polishMic;

    private boolean germanSource;

    TranslatorScreen(Activity activity, Presentation presentation, boolean germanSource, Actions actions) {
        this.activity = activity; this.presentation = presentation; this.germanSource = germanSource;
        boolean dark = presentation.dark();
        // Read current display/window pixels and density, including Fold and split-screen changes.
        android.util.DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int widthDp = Math.round(activity.getWindowManager().getCurrentWindowMetrics().getBounds().width() / metrics.density);
        boolean wide = widthDp >= 680;
        background = color(dark ? "#0B1115" : "#FFFBF6");
        surface = color(dark ? "#192329" : "#FFFFFF");
        foreground = color(dark ? "#E7EFF2" : "#172C2D");
        muted = color(dark ? "#A8BAC4" : "#607174");
        accent = color(dark ? "#168CE2" : "#087F80");
        border = color(dark ? "#35434D" : "#E9E5DD");
        int coral = color("#EE7564"), teal = color("#087F80");

        ScrollView scroll = new ScrollView(activity);
        scroll.setId(R.id.screen); scroll.setFillViewport(true); scroll.setBackgroundColor(background);
        scroll.setClipToPadding(true);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            scroll.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        LinearLayout outer = column(); outer.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout root = column(); root.setPadding(dp(16), dp(12), dp(16), dp(20)); root.setFocusableInTouchMode(true);
        outer.addView(root, new LinearLayout.LayoutParams(widthDp > 960 ? dp(960) : -1, -2)); scroll.addView(outer);
        activity.setContentView(scroll); scroll.requestApplyInsets();
        WindowInsetsController bars = activity.getWindow().getInsetsController();
        if (bars != null) bars.setSystemBarsAppearance(dark ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);

        LinearLayout header = row();
        TextView title = text("Sprechbrücke", 26, foreground); title.setTypeface(Typeface.create("serif", Typeface.BOLD));
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        modelBadge = text("● Modelle prüfen", 13, muted);
        modelBadge.setGravity(Gravity.CENTER_VERTICAL);
        if (widthDp >= 480) header.addView(modelBadge, new LinearLayout.LayoutParams(dp(116), -2));
        ImageButton settings = icon(R.drawable.ic_settings, "Einstellungen", Color.TRANSPARENT, foreground, v -> actions.settings());
        settings.setId(R.id.settings); header.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(header); if (widthDp < 480) root.addView(modelBadge); space(root, 8);

        boolean stackTabs = widthDp < 280;
        LinearLayout tabs = row(); if (stackTabs) tabs.setOrientation(LinearLayout.VERTICAL); tabs.setPadding(dp(2), dp(2), dp(2), dp(2)); tabs.setBackground(shape(dark ? surface : color("#F0EDE7"), 28));
        String[] tabLabels = {"Text", "Gespräch", "Live"};
        int[] tabIds = {R.id.mode_text, R.id.mode_conversation, R.id.mode_live};
        int[] tabIcons = {R.drawable.ic_text, R.drawable.ic_chat, R.drawable.ic_live};
        Presentation.Mode[] modes = Presentation.Mode.values();
        for (int i = 0; i < modes.length; i++) {
            Presentation.Mode mode = modes[i]; boolean selected = mode == presentation.mode;
            int selectedColor = !dark && mode == Presentation.Mode.CONVERSATION ? coral : accent;
            Button tab = button(tabLabels[i], selected ? selectedColor : Color.TRANSPARENT, selected ? Color.WHITE : foreground, v -> actions.mode(mode));
            tab.setId(tabIds[i]); setType(tab, 16); tab.setSelected(selected);
            android.graphics.drawable.Drawable tabIcon = activity.getDrawable(tabIcons[i]).mutate();
            tabIcon.setBounds(0, 0, dp(24), dp(24));
            tab.setCompoundDrawables(null, tabIcon, null, null);
            tab.setCompoundDrawablePadding(dp(4));
            tab.setCompoundDrawableTintList(ColorStateList.valueOf(selected ? Color.WHITE : foreground));
            tabs.addView(tab, stackTabs ? new LinearLayout.LayoutParams(-1, -2)
                : new LinearLayout.LayoutParams(0, -2, 1f));
        }
        root.addView(tabs); space(root, 10);
        LinearLayout languages = row();
        from = button("", surface, foreground, v -> actions.swap());
        to = button("", surface, foreground, v -> actions.swap());
        from.setContentDescription("Ausgangssprache wechseln"); to.setContentDescription("Zielsprache wechseln");
        ImageButton swap = icon(R.drawable.ic_swap, "Sprachrichtung wechseln", Color.TRANSPARENT, foreground, v -> actions.swap()); swap.setContentDescription("Sprachrichtung wechseln");
        languages.addView(from, new LinearLayout.LayoutParams(0, -2, 1));
        languages.addView(swap, new LinearLayout.LayoutParams(dp(48), dp(48)));
        languages.addView(to, new LinearLayout.LayoutParams(0, -2, 1)); root.addView(languages); space(root, 10);

        level = new LevelView(activity, color("#60C7FF"));
        if (presentation.isLive()) root.addView(level, new LinearLayout.LayoutParams(-1, dp(widthDp < 360 ? 62 : 78)));
        status = text("Sprachmodelle werden geprüft …", 14, muted); status.setGravity(Gravity.CENTER);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(status); space(root, 10);

        LinearLayout cards = new LinearLayout(activity);
        cards.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL); cards.setBaselineAligned(false);
        sourceCard = card(dark ? surface : color("#FCE9E3"));
        sourceLabel = text("", 14, muted); sourceCard.addView(sourceLabel);
        input = new EditText(activity); input.setId(R.id.input_text);
        input.setTextColor(foreground); input.setHintTextColor(muted); setType(input, presentation.isLive() ? 22 : 24);
        input.setHint(presentation.mode == Presentation.Mode.TEXT ? "Text eingeben …" : "Gesprochenes …");
        input.setMinLines(presentation.mode == Presentation.Mode.TEXT ? 3 : 1);
        input.setGravity(Gravity.TOP | Gravity.START); input.setPadding(0, dp(6), 0, dp(6));
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5000)}); input.setContentDescription("Ausgangstext");
        LinearLayout original = row();
        original.addView(input, new LinearLayout.LayoutParams(0, -2, 1));
        sourcePlay = icon(R.drawable.ic_volume, "Ausgangstext vorlesen", color("#F6CFC4"), color("#AD493B"), v -> actions.speakSource());
        if (!dark && presentation.mode == Presentation.Mode.CONVERSATION)
            original.addView(sourcePlay, new LinearLayout.LayoutParams(dp(48), dp(48)));
        sourceCard.addView(original);
        if (presentation.mode == Presentation.Mode.TEXT) sourceCard.addView(text("Bis zu 5.000 Zeichen", 14, muted));
        targetCard = card(dark ? color("#153C57") : color("#DDF0EC"));
        targetLabel = text("", 14, muted); targetCard.addView(targetLabel);
        LinearLayout translated = row();
        output = text("", 24, dark ? color("#BDE6FF") : foreground); output.setId(R.id.output_text);
        output.setHint("Übersetzung …"); output.setHintTextColor(muted); output.setMinLines(1);
        output.setTextIsSelectable(true); output.setPadding(0, dp(6), dp(8), dp(6));
        translated.addView(output, new LinearLayout.LayoutParams(0, -2, 1));
        play = icon(R.drawable.ic_volume, "Übersetzung vorlesen", dark ? color("#28618A") : color("#BFE3DB"), dark ? Color.WHITE : teal, v -> actions.speak());
        translated.addView(play, new LinearLayout.LayoutParams(dp(48), dp(48))); targetCard.addView(translated);
        if (wide) {
            LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, -2, 1); first.setMarginEnd(dp(14));
            cards.addView(sourceCard, first); cards.addView(targetCard, new LinearLayout.LayoutParams(0, -2, 1));
        } else { cards.addView(sourceCard); space(cards, 10); cards.addView(targetCard); }
        root.addView(cards); space(root, 14);

        microphone = roundButton("Mikrofon starten", accent, v -> actions.record(this.germanSource)); microphone.setId(R.id.main_mic);
        germanMic = roundButton("Deutsch\nsprechen", coral, v -> actions.record(true)); germanMic.setId(R.id.german_mic);
        polishMic = roundButton("Mów\npo polsku", teal, v -> actions.record(false)); polishMic.setId(R.id.polish_mic);
        if (presentation.mode == Presentation.Mode.TEXT) root.addView(button("Übersetzen", accent, Color.WHITE, v -> actions.translate()));
        else {
            LinearLayout microphones = row(); microphones.setGravity(Gravity.CENTER);
            int diameter = Math.min(144, (Math.min(widthDp, 600) - 56) / 2);
            // Stack only in narrow windows; system font settings never change this layout.
            if (presentation.dualMicrophones()) {
                boolean stack = widthDp < 320;
                microphones.setOrientation(stack ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
                int size = stack ? Math.min(widthDp - 40, 220) : diameter;
                microphones.addView(germanMic, new LinearLayout.LayoutParams(dp(size), -2));
                View gap = new View(activity); microphones.addView(gap, new LinearLayout.LayoutParams(dp(18), dp(12)));
                microphones.addView(polishMic, new LinearLayout.LayoutParams(dp(size), -2));
            } else microphones.addView(microphone, new LinearLayout.LayoutParams(dp(132), -2));
            root.addView(microphones);
        }
        readAloud = new Switch(activity); readAloud.setText("Automatisch vorlesen"); readAloud.setTextColor(muted);
        setType(readAloud, 16); readAloud.setMinHeight(dp(48)); readAloud.setThumbTintList(ColorStateList.valueOf(accent));
        if (presentation.mode == Presentation.Mode.CONVERSATION) root.addView(readAloud);
        if (presentation.isLive()) {
            TextView explanation = text("Übersetzung in Sprachabschnitten ⓘ", 14, muted); explanation.setGravity(Gravity.CENTER); explanation.setMinHeight(dp(48));
            explanation.setOnClickListener(v -> new android.app.AlertDialog.Builder(activity).setMessage("Während der Übersetzung pausiert das Mikrofon. Vorlesen beendet die Aufnahme.").setPositiveButton("OK", null).show());
            root.addView(explanation);
        }
        space(root, 8);
        LinearLayout voices = card(surface); voices.setPadding(dp(12), dp(8), dp(12), dp(8)); voices.setMinimumHeight(dp(48));
        TextView voiceTitle = text("Stimme & Hörprobe", 16, foreground); leadingIcon(voiceTitle, R.drawable.ic_volume); voices.addView(voiceTitle);
        voiceInfo = text("Sprachausgabe wird geladen …", 14, muted); voiceInfo.setMaxLines(2);
        voiceInfo.setEllipsize(android.text.TextUtils.TruncateAt.END); voices.addView(voiceInfo);
        voices.setBackground(ripple(surface, 16)); voices.setOnClickListener(v -> actions.voices()); voices.setFocusable(true); root.addView(voices);
        space(root, 10);
        LinearLayout historyCard = card(surface);
        TextView historyTitle = text("Letzte Unterhaltung", 16, foreground); leadingIcon(historyTitle, R.drawable.ic_history); historyCard.addView(historyTitle);
        history = text("", 16, foreground); history.setId(R.id.history_text); history.setHint("Hier erscheinen deine Übersetzungen."); history.setHintTextColor(muted);
        history.setPadding(0, dp(8), 0, dp(4)); history.setTextIsSelectable(true); historyCard.addView(history);
        Button clear = button("Verlauf löschen", Color.TRANSPARENT, muted, v -> actions.clear()); setType(clear, 16); historyCard.addView(clear); root.addView(historyCard);
        updateDirection(germanSource); root.requestFocus();
    }
    void updateDirection(boolean german) {
        germanSource = german;
        from.setText(german ? "🇩🇪 Deutsch" : "🇵🇱 Polski"); to.setText(german ? "🇵🇱 Polski" : "🇩🇪 Deutsch");
        sourceLabel.setText(german ? "Deutsch · Ausgangstext" : "Polski · Ausgangstext");
        targetLabel.setText(german ? "Polski · Übersetzung" : "Deutsch · Übersetzung");
        if (!presentation.dark()) {
            sourceCard.setBackground(shape(color(german ? "#FCE9E3" : "#DDF0EC"), 18));
            targetCard.setBackground(shape(color(german ? "#DDF0EC" : "#FCE9E3"), 18));
            sourcePlay.setBackground(ripple(color(german ? "#F6CFC4" : "#BFE3DB"), 100));
            sourcePlay.setImageTintList(ColorStateList.valueOf(color(german ? "#AD493B" : "#096C6D")));
            sourceLabel.setTextColor(color(german ? "#AD493B" : "#096C6D")); targetLabel.setTextColor(color(german ? "#096C6D" : "#AD493B"));
            play.setBackground(ripple(color(german ? "#BFE3DB" : "#F6CFC4"), 100));
            play.setImageTintList(ColorStateList.valueOf(color(german ? "#096C6D" : "#AD493B")));
        }
    }
    void setModelsReady(boolean ready) {
        modelBadge.setText(ready ? "● Text offline" : "● Modelle fehlen");
        modelBadge.setTextColor(ready ? color(presentation.dark() ? "#5DD5A0" : "#16744F") : muted);
        modelBadge.setContentDescription(ready ? "Textübersetzung offline verfügbar" : "Übersetzungsmodelle noch nicht bereit");
    }
    void setRecording(boolean running) {
        microphone.setText(running ? "Stoppen" : "Mikrofon starten");
        germanMic.setText(running && germanSource ? "Aufnahme\nstoppen" : "Deutsch\nsprechen");
        polishMic.setText(running && !germanSource ? "Zatrzymaj\nnagrywanie" : "Mów\npo polsku");
        microphone.setContentDescription(running ? "Aufnahme stoppen" : "Mikrofon starten");
        if (!running) level.reset();
    }
    private void setType(TextView view, float size) {
        view.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, size);
    }
    private void leadingIcon(TextView view, int resource) {
        android.graphics.drawable.Drawable drawable = activity.getDrawable(resource).mutate();
        drawable.setBounds(0, 0, dp(24), dp(24));
        view.setCompoundDrawables(drawable, null, null, null);
        view.setCompoundDrawablePadding(dp(8));
        view.setCompoundDrawableTintList(ColorStateList.valueOf(foreground));
    }
    private int color(String value) { return Color.parseColor(value); }
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout v = new LinearLayout(activity); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(activity); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private void space(LinearLayout parent, int height) { parent.addView(new View(activity), new LinearLayout.LayoutParams(1, dp(height))); }
    private TextView text(String value, int size, int color) { TextView v = new TextView(activity); v.setText(value); v.setTextColor(color); setType(v, size); return v; }
    private LinearLayout card(int color) { LinearLayout v = column(); v.setPadding(dp(14), dp(12), dp(14), dp(12)); v.setBackground(shape(color, 18)); return v; }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); if (color == surface) d.setStroke(dp(1), border); return d;
    }
    private RippleDrawable ripple(int color, int radius) { return new RippleDrawable(ColorStateList.valueOf(presentation.dark() ? 0x3354B9FF : 0x22087D80), shape(color, radius), shape(Color.WHITE, radius)); }
    private ImageButton icon(int resource, String label, int fill, int tint, View.OnClickListener listener) {
        ImageButton v = new ImageButton(activity); v.setImageResource(resource); v.setImageTintList(ColorStateList.valueOf(tint));
        v.setContentDescription(label); v.setBackground(ripple(fill, 100)); v.setPadding(dp(10), dp(10), dp(10), dp(10)); v.setOnClickListener(listener); return v;
    }
    private void styleButton(Button v, String title, int fill, int tint, View.OnClickListener listener) {
        v.setText(title); v.setAllCaps(false); v.setTextColor(tint); setType(v, 16); v.setGravity(Gravity.CENTER);
        v.setMinHeight(dp(48)); v.setMinimumHeight(dp(48)); v.setMinWidth(0); v.setMinimumWidth(0);
        v.setPadding(dp(8), dp(6), dp(8), dp(6)); v.setBackground(ripple(fill, 28)); v.setStateListAnimator(null); v.setOnClickListener(listener);
    }
    private Button button(String title, int fill, int tint, View.OnClickListener listener) { Button v = new Button(activity); styleButton(v, title, fill, tint, listener); return v; }
    // This app uses platform Activity/Material themes on API 31+, not AppCompat.
    @android.annotation.SuppressLint("AppCompatCustomView")
    private Button roundButton(String title, int fill, View.OnClickListener listener) {
        Button v = new Button(activity) {
            @Override protected void onMeasure(int widthSpec, int heightSpec) {
                setPadding(dp(18), dp(14), dp(18), dp(14));
                super.onMeasure(widthSpec, heightSpec);
                // Center the icon and text as one group, while letting large text grow the pill.
                int desiredHeight = getMeasuredHeight();
                int height = Math.max(getMeasuredWidth(), desiredHeight);
                int extra = (height - desiredHeight) / 2;
                setPadding(dp(18), dp(14) + extra, dp(18), dp(14) + extra);
                super.onMeasure(widthSpec, View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            }
        };
        styleButton(v, title, fill, Color.WHITE, listener); v.setPadding(dp(18), dp(14), dp(18), dp(14));
        android.graphics.drawable.Drawable mic = activity.getDrawable(R.drawable.ic_mic).mutate();
        mic.setBounds(0, 0, dp(40), dp(40)); v.setCompoundDrawables(null, mic, null, null); v.setCompoundDrawableTintList(ColorStateList.valueOf(Color.WHITE)); v.setCompoundDrawablePadding(dp(6));
        v.setBackground(ripple(fill, 150)); return v;
    }
}
