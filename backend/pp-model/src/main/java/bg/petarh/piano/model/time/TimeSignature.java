package bg.petarh.piano.model.time;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeSignature {

    public static final TimeSignature COMMON_TIME = new TimeSignature(4, 4);
    public static final List<TimeSignature> PRACTICE_METERS =
            List.of(new TimeSignature(2, 4), new TimeSignature(3, 4), COMMON_TIME, new TimeSignature(6, 8));

    private static final int MOST_BEATS = 32;
    private static final int SHORTEST_BEAT_UNIT = 64;
    private static final int BEATS_PER_COMPOUND_GROUP = 3;
    private static final Pattern NOTATION = Pattern.compile("(\\d{1,2})/(\\d{1,2})");

    private final int beats;
    private final int beatUnit;

    private TimeSignature(int beats, int beatUnit) {
        this.beats = beats;
        this.beatUnit = beatUnit;
    }

    public static TimeSignature of(int beats, int beatUnit) {
        if (beats < 1 || beats > MOST_BEATS) {
            throw new IllegalArgumentException("Beats per measure out of range 1-" + MOST_BEATS + ": " + beats);
        }
        if (beatUnit < 1 || beatUnit > SHORTEST_BEAT_UNIT || Integer.bitCount(beatUnit) != 1) {
            throw new IllegalArgumentException("Beat unit must be a note value (1, 2, 4, ... 64): " + beatUnit);
        }
        return new TimeSignature(beats, beatUnit);
    }

    public static TimeSignature parse(String notation) {
        Matcher matcher = NOTATION.matcher(notation);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time signature: " + notation);
        }
        return of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    public int beats() {
        return beats;
    }

    public int beatUnit() {
        return beatUnit;
    }

    public Fraction measureLength() {
        return Fraction.of(beats, beatUnit);
    }

    public boolean isCompound() {
        return beats > BEATS_PER_COMPOUND_GROUP && beats % BEATS_PER_COMPOUND_GROUP == 0;
    }

    public Fraction countedBeat() {
        return Fraction.of(isCompound() ? BEATS_PER_COMPOUND_GROUP : 1, beatUnit);
    }

    public int beatsPerMeasure() {
        return isCompound() ? beats / BEATS_PER_COMPOUND_GROUP : beats;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TimeSignature signature
                && signature.beats == beats
                && signature.beatUnit == beatUnit;
    }

    @Override
    public int hashCode() {
        return beats * 31 + beatUnit;
    }

    @Override
    public String toString() {
        return beats + "/" + beatUnit;
    }
}
