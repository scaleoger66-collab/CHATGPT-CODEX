package de.sprechbruecke;

/** Designs choose their matching starting mode; all three modes remain available. */
final class Presentation {
    enum Design { NIGHT, DIALOG }
    enum Mode { TEXT, CONVERSATION, LIVE }
    Design design;
    Mode mode;

    Presentation(String savedDesign, String savedMode) {
        try { design = Design.valueOf(savedDesign); }
        catch (IllegalArgumentException | NullPointerException e) { design = Design.DIALOG; }
        try { mode = Mode.valueOf(savedMode); }
        catch (IllegalArgumentException | NullPointerException e) { mode = defaultMode(design); }
    }
    static Mode defaultMode(Design design) { return design == Design.NIGHT ? Mode.LIVE : Mode.CONVERSATION; }
    void chooseDesign(Design design) { this.design = design; mode = defaultMode(design); }
    boolean dualMicrophones() { return design == Design.DIALOG && mode == Mode.CONVERSATION; }
    boolean isLive() { return mode == Mode.LIVE; }
    boolean dark() { return design == Design.NIGHT; }
}
