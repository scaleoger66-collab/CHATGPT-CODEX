package de.sprechbruecke.voices;

/** Pinned model downloads from the upstream sherpa-onnx release, verified with SHA-256. */
final class PiperVoice {
    static final PiperVoice GOSIA = new PiperVoice("gosia", "Gosia", "pl", "pl_PL-gosia-medium", 67211182L,
        "75bd34dcbdc4dd98d763954756b4b34b4208100497c836381542e4d73dcefa9c");
    static final PiperVoice DARKMAN = new PiperVoice("darkman", "Darkman", "pl", "pl_PL-darkman-medium", 67215635L,
        "444727aa46eb6db645a2bc88fe73868e4bd7596b7f56ca39fad5ef53c41210d4");
    static final PiperVoice THORSTEN = new PiperVoice("thorsten", "Thorsten High", "de", "de_DE-thorsten-high", 115591546L,
        "dd4ed1b0d42c30a1a4862fc2b243e8044d52b8889c9ff3d1e99e92028888bc4a");
    static final PiperVoice[] ALL = {GOSIA, DARKMAN, THORSTEN};
    final String id, name, language, model, sha256;
    final long bytes;
    private PiperVoice(String id, String name, String language, String model, long bytes, String sha256) {
        this.id=id; this.name=name; this.language=language; this.model=model; this.bytes=bytes; this.sha256=sha256;
    }
    String directory() { return "vits-piper-" + model; }
    String url() { return "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/" + directory() + ".tar.bz2"; }
    String languageName() { return language.equals("pl") ? "Polski" : "Deutsch"; }
    static PiperVoice forLanguage(String language, String polishChoice) {
        if (language.equals("de")) return THORSTEN;
        if (language.equals("pl")) return DARKMAN.id.equals(polishChoice) ? DARKMAN : GOSIA;
        throw new IllegalArgumentException("Unsupported speech language: " + language);
    }
}
