package de.sprechbruecke;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Metadata-only selection, also testable without an Android speech service. */
final class OfflineVoiceChoice {
    final String name, language, tag;
    final int quality;
    final boolean network, installed;

    OfflineVoiceChoice(String name, String language, String tag, int quality, boolean network, boolean installed) {
        this.name = name; this.language = language; this.tag = tag;
        this.quality = quality; this.network = network; this.installed = installed;
    }

    static List<OfflineVoiceChoice> available(List<OfflineVoiceChoice> voices, String language) {
        List<OfflineVoiceChoice> result = new ArrayList<>();
        for (OfflineVoiceChoice voice : voices)
            if (voice.installed && !voice.network && voice.language.equals(language)) result.add(voice);
        result.sort(Comparator.comparingInt((OfflineVoiceChoice v) -> v.quality).reversed()
            .thenComparing(v -> v.name));
        return result;
    }

    static OfflineVoiceChoice select(List<OfflineVoiceChoice> available, String savedName) {
        if (!savedName.isEmpty()) {
            for (OfflineVoiceChoice voice : available) if (voice.name.equals(savedName)) return voice;
            return null; // Never silently substitute a voice the user chose explicitly.
        }
        return available.isEmpty() ? null : available.get(0);
    }
}
