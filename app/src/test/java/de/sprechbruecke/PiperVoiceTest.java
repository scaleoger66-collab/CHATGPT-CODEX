package de.sprechbruecke;
import org.junit.Test;
import static org.junit.Assert.*;
public class PiperVoiceTest {
    @Test public void directionUsesCorrectLanguageAndPreservesPolishChoice() {
        assertSame(PiperVoice.GOSIA,PiperVoice.forLanguage("pl","gosia"));
        assertSame(PiperVoice.DARKMAN,PiperVoice.forLanguage("pl","darkman"));
        assertSame(PiperVoice.THORSTEN,PiperVoice.forLanguage("de","darkman"));
        assertSame(PiperVoice.DARKMAN,PiperVoice.forLanguage("pl","darkman"));
    }
    @Test(expected=IllegalArgumentException.class) public void unsupportedLanguageCannotSpeakInWrongLanguage() { PiperVoice.forLanguage("en","gosia"); }
}
