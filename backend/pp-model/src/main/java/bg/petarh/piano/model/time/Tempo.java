package bg.petarh.piano.model.time;

import java.util.Objects;

public final class Tempo {

    public static final int SLOWEST_BPM = 20;
    public static final int FASTEST_BPM = 240;

    private static final Fraction QUARTER = Fraction.of(1, 4);
    private static final long NANOS_PER_MINUTE = 60_000_000_000L;

    private final int bpm;
    private final Fraction beatUnit;

    private Tempo(int bpm, Fraction beatUnit) {
        this.bpm = bpm;
        this.beatUnit = beatUnit;
    }

    public static Tempo of(int bpm) {
        return of(bpm, QUARTER);
    }

    public static Tempo of(int bpm, Fraction beatUnit) {
        if (bpm < SLOWEST_BPM || bpm > FASTEST_BPM) {
            throw new IllegalArgumentException("Tempo out of range " + SLOWEST_BPM + "-" + FASTEST_BPM + " BPM: " + bpm);
        }
        if (!beatUnit.isPositive()) {
            throw new IllegalArgumentException("Beat unit must be positive: " + beatUnit);
        }
        return new Tempo(bpm, beatUnit);
    }

    public int bpm() {
        return bpm;
    }

    public Fraction beatUnit() {
        return beatUnit;
    }

    public TempoMarking marking() {
        return TempoMarking.forBpm(bpm);
    }

    public Tempo withBpm(int newBpm) {
        return of(newBpm, beatUnit);
    }

    public Tempo adjustedBy(int deltaBpm) {
        return withBpm(Math.clamp((long) bpm + deltaBpm, SLOWEST_BPM, FASTEST_BPM));
    }

    public long nanosFor(Fraction duration) {
        long numerator = Math.multiplyExact(
                Math.multiplyExact(duration.numerator(), beatUnit.denominator()), NANOS_PER_MINUTE);
        long denominator = Math.multiplyExact(
                Math.multiplyExact(duration.denominator(), beatUnit.numerator()), bpm);
        return Math.floorDiv(Math.addExact(Math.multiplyExact(numerator, 2), denominator),
                Math.multiplyExact(denominator, 2));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Tempo tempo && tempo.bpm == bpm && tempo.beatUnit.equals(beatUnit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bpm, beatUnit);
    }

    @Override
    public String toString() {
        return bpm + " BPM (" + marking().displayName() + ")";
    }
}
