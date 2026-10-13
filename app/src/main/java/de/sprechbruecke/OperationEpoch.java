package de.sprechbruecke;

/** Main-thread token for discarding callbacks after a new operation or cancellation. */
final class OperationEpoch {
    private long value;
    long advance() { return ++value; }
    long current() { return value; }
    boolean accepts(long token) { return value == token; }
}
