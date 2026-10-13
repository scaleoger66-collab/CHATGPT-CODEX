package de.sprechbruecke;

import org.junit.Test;
import static org.junit.Assert.*;

public class PresentationTest {
    @Test public void designSwitchChangesBehaviorInBothDirections() {
        Presentation state = new Presentation("DIALOG", "TEXT");
        state.chooseDesign(Presentation.Design.NIGHT);
        assertTrue(state.dark()); assertTrue(state.isLive()); assertFalse(state.dualMicrophones());
        state.chooseDesign(Presentation.Design.DIALOG);
        assertFalse(state.dark()); assertFalse(state.isLive()); assertTrue(state.dualMicrophones());
    }
    @Test public void savedExplicitModeSurvivesRestart() {
        Presentation state = new Presentation("NIGHT", "TEXT");
        assertTrue(state.dark()); assertEquals(Presentation.Mode.TEXT, state.mode);
    }
    @Test public void absentOrInvalidPreferencesUseSafeDefaults() {
        Presentation state = new Presentation(null, "invalid");
        assertTrue(state.dualMicrophones()); assertFalse(state.isLive());
        assertTrue(new Presentation("NIGHT", null).isLive());
    }
}
