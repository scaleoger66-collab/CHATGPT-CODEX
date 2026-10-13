package de.sprechbruecke;

import org.junit.Test;
import static org.junit.Assert.*;

public class OperationEpochTest {
    @Test public void stoppingInvalidatesAnOutstandingResult() {
        OperationEpoch epoch = new OperationEpoch();
        long recording = epoch.advance();
        epoch.advance();
        assertFalse(epoch.accepts(recording));
    }
    @Test public void aLateResultCannotReplaceANewerTranslation() {
        OperationEpoch epoch = new OperationEpoch();
        long first = epoch.advance();
        long second = epoch.advance();
        assertFalse(epoch.accepts(first));
        assertTrue(epoch.accepts(second));
    }
    @Test public void readingTheCurrentTokenDoesNotCancelTheOperation() {
        OperationEpoch epoch = new OperationEpoch();
        long token = epoch.advance();
        assertEquals(token, epoch.current());
        assertTrue(epoch.accepts(token));
    }
}
