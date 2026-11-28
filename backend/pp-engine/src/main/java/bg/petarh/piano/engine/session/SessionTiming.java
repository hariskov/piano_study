package bg.petarh.piano.engine.session;

import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TempoMap;
import bg.petarh.piano.model.time.TimeSignature;

import java.util.Objects;

public record SessionTiming(TempoMap tempoMap, TimeSignature meter, int countInBars) {

    private static final int MOST_COUNT_IN_BARS = 2;

    public SessionTiming {
        Objects.requireNonNull(tempoMap, "tempoMap");
        Objects.requireNonNull(meter, "meter");
        if (countInBars < 0 || countInBars > MOST_COUNT_IN_BARS) {
            throw new IllegalArgumentException("Count-in is off, 1 or 2 bars: " + countInBars);
        }
    }

    public SessionTiming(Tempo tempo, TimeSignature meter, int countInBars) {
        this(TempoMap.constant(Objects.requireNonNull(tempo, "tempo")), meter, countInBars);
    }

    public Tempo tempo() {
        return tempoMap.tempoAt(Fraction.ZERO);
    }

    public long countInNanos() {
        return tempo().nanosFor(meter.measureLength().times(countInBars));
    }
}
