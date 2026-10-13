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

/** Native layouts shared by the two selectable presentations. Sizes are density-independent. */
final class TranslatorScreen {
    interface Actions {
        void settings();
        void mode(Presentation.Mode mode);
        void swap();
        void record(boolean german);
        void translate();
        void speak();
        void voices();
        void clear();
    }
    final EditText input;
    final TextView output, status, history, voiceInfo;
    final Switch readAloud;
    final LevelView level;
    private final Activity activity;
    private final Presentation presentation;
    private final int background, surface, foreground, muted, accent, border;
    private final TextView sourceLabel, targetLabel, direction;
    private final Button microphone, germanMic, polishMic;
    private boolean germanSource;

    TranslatorScreen(Activity activity, Presentation presentation, boolean germanSource, Actions actions) {
        this.activity = activity; this.presentation = presentation; this.germanSource = germanSource;
        boolean dark = presentation.dark();
        background = color(dark ? "#0B1219" : "#FAF7F2");
        surface = color(dark ? "#17222D" : "#FFFFFF");
        foreground = color(dark ? "#E4F1FC" : "#143338");
        muted = color(dark ? "#A9BDCC" : "#536B71");
        accent = color(dark ? "#146DB6" : "#087D80");
        border = color(dark ? "#2F4455" : "#E5E2DA");
        int coral = color("#C54B3B");
        int teal = color("#087D80");

        ScrollView scroll = new ScrollView(activity);
        scroll.setId(R.id.screen); scroll.setFillViewport(true); scroll.setBackgroundColor(background);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            scroll.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
        });
        // Center the content on the unfolded display, while allowing all controls to scroll at large font sizes.
        LinearLayout outer = column(); outer.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout root = column(); root.setPadding(dp(20), dp(24), dp(20), dp(28));
        int widthDp = activity.getResources().getConfiguration().screenWidthDp;
        outer.addView(root, new LinearLayout.LayoutParams(widthDp > 900 ? dp(900) : -1, -2)); scroll.addView(outer);
        activity.setContentView(scroll);
        WindowInsetsController bars = activity.getWindow().getInsetsController();
        if (bars != null) bars.setSystemBarsAppearance(dark ? 0 : WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);

        LinearLayout header = row();
        LinearLayout heading = column();
        TextView title = text("Sprechbrücke", 28, foreground); title.setTypeface(Typeface.create("serif", Typeface.BOLD));
        heading.addView(title);
        heading.addView(text(dark ? "NACHT & FOKUS" : "DIALOG & NÄHE", 11, muted));
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        ImageButton settings = new ImageButton(activity); settings.setId(R.id.settings);
        settings.setImageResource(R.drawable.ic_settings); settings.setImageTintList(ColorStateList.valueOf(foreground));
        settings.setContentDescription("Einstellungen"); settings.setBackground(ripple(surface, 24));
        settings.setOnClickListener(v -> actions.settings()); header.addView(settings, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(header); space(root, 20);

        LinearLayout tabs = row(); tabs.setPadding(dp(4), dp(4), dp(4), dp(4)); tabs.setBackground(shape(dark ? surface : color("#EDEAE4"), 28));
        String[] tabLabels = {"Text", "Gespräch", "Live"};
        int[] tabIds = {R.id.mode_text, R.id.mode_conversation, R.id.mode_live};
        Presentation.Mode[] modes = Presentation.Mode.values();
        for (int i = 0; i < modes.length; i++) {
            Presentation.Mode mode = modes[i]; boolean selected = mode == presentation.mode;
            Button tab = button(tabLabels[i], selected ? accent : Color.TRANSPARENT, selected ? Color.WHITE : muted, v -> actions.mode(mode));
            tab.setId(tabIds[i]); tab.setTextSize(14); tab.setSelected(selected);
            tabs.addView(tab, new LinearLayout.LayoutParams(0, -2, 1));
        }
        root.addView(tabs); space(root, 16);
        direction = button("", surface, foreground, v -> actions.swap());
        direction.setContentDescription("Sprachrichtung wechseln"); root.addView(direction); space(root, 10);

        level = new LevelView(activity, accent);
        if (presentation.isLive()) root.addView(level, new LinearLayout.LayoutParams(-1, dp(74)));
        status = text("Sprachmodelle werden geprüft …", 14, muted); status.setGravity(Gravity.CENTER);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(status); space(root, 16);

        LinearLayout cards = new LinearLayout(activity);
        boolean wide = widthDp >= 720;
        cards.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        cards.setBaselineAligned(false);
        LinearLayout sourceCard = card(dark ? surface : color("#FCE7E0"));
        sourceLabel = text("", 13, dark ? muted : coral); sourceCard.addView(sourceLabel);
        input = new EditText(activity); input.setId(R.id.input_text);
        input.setTextColor(foreground); input.setHintTextColor(muted); input.setTextSize(22);
        input.setHint(presentation.mode == Presentation.Mode.TEXT ? "Text eingeben …" : "Gesprochenes erscheint hier …");
        input.setMinLines(2); input.setGravity(Gravity.TOP | Gravity.START); input.setPadding(0, dp(8), 0, dp(8));
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5000)});
        input.setContentDescription("Ausgangstext"); sourceCard.addView(input);
        TextView inputNote = text("Bis zu 5.000 Zeichen · bearbeitbar", 11, muted); sourceCard.addView(inputNote);
        LinearLayout targetCard = card(dark ? color("#102D45") : color("#D9EEEA"));
        targetLabel = text("", 13, dark ? color("#A2D5FA") : teal); targetCard.addView(targetLabel);
        output = text("", 23, dark ? color("#C4E7FF") : foreground); output.setId(R.id.output_text);
        output.setHint("Deine Übersetzung …"); output.setHintTextColor(muted); output.setMinLines(2);
        output.setTextIsSelectable(true); output.setPadding(0, dp(12), 0, dp(16)); targetCard.addView(output);
        Button play = button("Vorlesen", dark ? color("#204868") : color("#BCE0DA"), foreground, v -> actions.speak());
        play.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_volume, 0, 0, 0);
        play.setCompoundDrawableTintList(ColorStateList.valueOf(foreground)); play.setContentDescription("Übersetzung vorlesen"); targetCard.addView(play);
        if (wide) {
            LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, -2, 1); first.setMarginEnd(dp(12));
            cards.addView(sourceCard, first); cards.addView(targetCard, new LinearLayout.LayoutParams(0, -2, 1));
        } else {
            cards.addView(sourceCard); space(cards, 12); cards.addView(targetCard);
        }
        root.addView(cards); space(root, 18);

        microphone = button("Mikrofon starten", accent, Color.WHITE, v -> actions.record(this.germanSource));
        microphone.setId(R.id.main_mic); micIcon(microphone);
        germanMic = button("Deutsch\nsprechen", coral, Color.WHITE, v -> actions.record(true)); germanMic.setId(R.id.german_mic); micIcon(germanMic);
        polishMic = button("Mów\npo polsku", teal, Color.WHITE, v -> actions.record(false)); polishMic.setId(R.id.polish_mic); micIcon(polishMic);
        if (presentation.mode == Presentation.Mode.TEXT) {
            root.addView(button("Übersetzen", accent, Color.WHITE, v -> actions.translate()));
        } else if (presentation.dualMicrophones()) {
            LinearLayout microphones = row();
            germanMic.setMinHeight(dp(112)); polishMic.setMinHeight(dp(112));
            LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, -2, 1); first.setMarginEnd(dp(12));
            microphones.addView(germanMic, first); microphones.addView(polishMic, new LinearLayout.LayoutParams(0, -2, 1));
            root.addView(microphones);
        } else root.addView(microphone);
        readAloud = new Switch(activity); readAloud.setText("Im Gespräch automatisch vorlesen");
        readAloud.setTextColor(muted); readAloud.setTextSize(14); readAloud.setMinHeight(dp(48));
        readAloud.setThumbTintList(ColorStateList.valueOf(accent));
        if (presentation.mode == Presentation.Mode.CONVERSATION) { space(root, 8); root.addView(readAloud); }
        if (presentation.isLive()) {
            space(root, 10);
            root.addView(text("Übersetzung in Sprachabschnitten. Während der Übersetzung pausiert das Mikrofon. Vorlesen beendet die Aufnahme.", 12, muted));
        }
        space(root, 18);
        Button voices = button("Stimme & Hörprobe  ›", surface, foreground, v -> actions.voices()); root.addView(voices);
        voiceInfo = text("Sprachausgabe wird geladen …", 12, muted); voiceInfo.setPadding(dp(8), dp(6), dp(8), 0); root.addView(voiceInfo);
        space(root, 22);
        LinearLayout historyCard = card(surface);
        TextView historyTitle = text("Letzte Unterhaltung", 16, foreground); historyTitle.setTypeface(null, Typeface.BOLD); historyCard.addView(historyTitle);
        historyCard.addView(text("Nur für diese Sitzung", 12, muted));
        history = text("", 15, foreground); history.setId(R.id.history_text); history.setHint("Deine Übersetzungen erscheinen hier."); history.setHintTextColor(muted);
        history.setPadding(0, dp(14), 0, dp(10)); history.setTextIsSelectable(true); historyCard.addView(history);
        historyCard.addView(button("Verlauf löschen", Color.TRANSPARENT, muted, v -> actions.clear())); root.addView(historyCard);
        space(root, 16); root.addView(text("Deutsch ↔ Polski · Offline nach Modelldownload\nÜbersetzungen können Fehler enthalten.", 12, muted));
        updateDirection(germanSource);
    }
    void updateDirection(boolean german) {
        germanSource = german;
        direction.setText(german ? "Deutsch   ⇄   Polski" : "Polski   ⇄   Deutsch");
        sourceLabel.setText(german ? "Deutsch · Ausgangstext" : "Polski · Ausgangstext");
        targetLabel.setText(german ? "Polski · Übersetzung" : "Deutsch · Übersetzung");
    }
    void setRecording(boolean running) {
        microphone.setText(running ? "Aufnahme stoppen" : "Mikrofon starten");
        germanMic.setText(running && germanSource ? "Aufnahme\nstoppen" : "Deutsch\nsprechen");
        polishMic.setText(running && !germanSource ? "Zatrzymaj\nnagrywanie" : "Mów\npo polsku");
        if (!running) level.reset();
    }
    private void micIcon(Button button) {
        button.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_mic, 0, 0); button.setCompoundDrawablePadding(dp(6));
    }
    private int color(String color) { return Color.parseColor(color); }
    private int dp(int value) { return Math.round(value * activity.getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout view = new LinearLayout(activity); view.setOrientation(LinearLayout.VERTICAL); return view; }
    private LinearLayout row() { LinearLayout view = new LinearLayout(activity); view.setOrientation(LinearLayout.HORIZONTAL); view.setGravity(Gravity.CENTER_VERTICAL); return view; }
    private void space(LinearLayout parent, int height) { parent.addView(new View(activity), new LinearLayout.LayoutParams(1, dp(height))); }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(activity); view.setText(value); view.setTextColor(color); view.setTextSize(size); return view;
    }
    private LinearLayout card(int color) {
        LinearLayout view = column(); view.setPadding(dp(18), dp(16), dp(18), dp(16)); view.setBackground(shape(color, 24)); return view;
    }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius));
        if (color == surface) drawable.setStroke(dp(1), border); return drawable;
    }
    private RippleDrawable ripple(int color, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(presentation.dark() ? 0x3354B9FF : 0x22087D80), shape(color, radius), shape(Color.WHITE, radius));
    }
    private Button button(String title, int color, int textColor, View.OnClickListener listener) {
        Button view = new Button(activity); view.setText(title); view.setAllCaps(false); view.setTextColor(textColor); view.setTextSize(16);
        view.setMinHeight(dp(52)); view.setMinWidth(0); view.setMinimumWidth(0); view.setPadding(dp(14), dp(12), dp(14), dp(12));
        view.setBackground(ripple(color, 28)); view.setStateListAnimator(null); view.setOnClickListener(listener); return view;
    }
}
