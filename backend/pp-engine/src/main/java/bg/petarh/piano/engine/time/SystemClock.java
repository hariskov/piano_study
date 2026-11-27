package bg.petarh.piano.engine.time;

public enum SystemClock implements Clock {
    INSTANCE;

    @Override
    public long nowNanos() {
        return System.nanoTime();
    }
}
