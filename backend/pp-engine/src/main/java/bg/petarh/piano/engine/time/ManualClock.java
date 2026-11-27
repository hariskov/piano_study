package bg.petarh.piano.engine.time;

import java.time.Duration;

public final class ManualClock implements Clock {

    private long nowNanos;

    public ManualClock() {
    }

    @Override
    public long nowNanos() {
        return nowNanos;
    }

    public void advance(Duration duration) {
        advanceNanos(duration.toNanos());
    }

    public void advanceNanos(long nanos) {
        if (nanos < 0) {
            throw new IllegalArgumentException("A clock never goes back: " + nanos + " ns");
        }
        nowNanos += nanos;
    }
}
