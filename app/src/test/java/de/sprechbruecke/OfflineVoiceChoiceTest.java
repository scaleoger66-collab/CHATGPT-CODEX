package de.sprechbruecke;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class OfflineVoiceChoiceTest {
    private OfflineVoiceChoice voice(String name, String language, int quality, boolean network, boolean installed) {
        return new OfflineVoiceChoice(name, language, language, quality, network, installed);
    }
    @Test public void excludesCloudMissingDataAndWrongLanguageEvenAtHigherQuality() {
        List<OfflineVoiceChoice> candidates = OfflineVoiceChoice.available(Arrays.asList(
            voice("cloud", "pl", 500, true, true), voice("missing", "pl", 500, false, false),
            voice("german", "de", 500, false, true), voice("local", "pl", 200, false, true)), "pl");
        assertEquals(1, candidates.size());
        assertEquals("local", candidates.get(0).name);
    }
    @Test public void automaticSelectionPrefersReportedQuality() {
        List<OfflineVoiceChoice> candidates = OfflineVoiceChoice.available(Arrays.asList(
            voice("basic", "pl", 100, false, true), voice("better", "pl", 400, false, true)), "pl");
        assertEquals("better", OfflineVoiceChoice.select(candidates, "").name);
        assertEquals("basic", OfflineVoiceChoice.select(candidates, "basic").name);
    }
    @Test public void missingSavedVoiceRequiresUserSelection() {
        List<OfflineVoiceChoice> candidates = OfflineVoiceChoice.available(Arrays.asList(
            voice("other", "pl", 400, false, true)), "pl");
        assertNull(OfflineVoiceChoice.select(candidates, "removed"));
    }
}
