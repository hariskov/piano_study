package bg.petarh.piano.engine.session;

import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;

import java.util.Objects;

public record SessionTiming(Tempo tempo, TimeSignature meter, int countInBars) {

    private static final int MOST_COUNT_IN_BARS = 2;

    public SessionTiming {
        Objects.requireNonNull(tempo, "tempo");
        Objects.requireNonNull(meter, "meter");
        if (countInBars < 0 || countInBars > MOST_COUNT_IN_BARS) {
            throw new IllegalArgumentException("Count-in is off, 1 or 2 bars: " + countInBars);
        }
    }

    public long countInNanos() {
        return tempo.nanosFor(meter.measureLength().times(countInBars));
    }
}
