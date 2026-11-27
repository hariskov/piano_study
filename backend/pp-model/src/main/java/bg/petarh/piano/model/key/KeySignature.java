package bg.petarh.piano.model.key;

import bg.petarh.piano.model.pitch.Pitch;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class KeySignature {

    private static final int MOST_ACCIDENTALS = 7;
    private static final int SEMITONES_PER_FIFTH = 7;
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int MINOR_TONIC_ABOVE_MAJOR = 9;
    private static final List<Integer> MAJOR_SCALE_STEPS = List.of(0, 2, 4, 5, 7, 9, 11);
    private static final List<String> TONICS_BY_FIFTHS = List.of(
            "Fb", "Cb", "Gb", "Db", "Ab", "Eb", "Bb", "F", "C", "G", "D", "A", "E", "B", "F#", "C#", "G#", "D#", "A#");
    private static final int MAJOR_TONIC_INDEX_OFFSET = TONICS_BY_FIFTHS.indexOf("C");
    private static final int MINOR_TONIC_INDEX_OFFSET = TONICS_BY_FIFTHS.indexOf("A");

    private final int fifths;
    private final KeyMode mode;
    private final Set<Integer> pitchClasses;

    private KeySignature(int fifths, KeyMode mode) {
        this.fifths = fifths;
        this.mode = mode;
        int majorTonic = majorTonicPitchClass(fifths);
        this.pitchClasses = MAJOR_SCALE_STEPS.stream()
                .map(step -> (majorTonic + step) % SEMITONES_PER_OCTAVE)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static KeySignature of(int fifths, KeyMode mode) {
        if (Math.abs(fifths) > MOST_ACCIDENTALS) {
            throw new IllegalArgumentException(
                    "A key signature has at most " + MOST_ACCIDENTALS + " sharps or flats: " + fifths);
        }
        return new KeySignature(fifths, mode);
    }

    public static KeySignature major(String tonic) {
        return fromTonic(tonic, KeyMode.MAJOR);
    }

    public static KeySignature minor(String tonic) {
        return fromTonic(tonic, KeyMode.MINOR);
    }

    public int fifths() {
        return fifths;
    }

    public KeyMode mode() {
        return mode;
    }

    public String tonicName() {
        return TONICS_BY_FIFTHS.get(fifths + indexOffset(mode));
    }

    public int tonicPitchClass() {
        int majorTonic = majorTonicPitchClass(fifths);
        return mode == KeyMode.MAJOR ? majorTonic : (majorTonic + MINOR_TONIC_ABOVE_MAJOR) % SEMITONES_PER_OCTAVE;
    }

    public Set<Integer> pitchClasses() {
        return pitchClasses;
    }

    public boolean contains(Pitch pitch) {
        return pitchClasses.contains(pitch.pitchClass());
    }

    public boolean usesFlats() {
        return fifths < 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof KeySignature key && key.fifths == fifths && key.mode == mode;
    }

    @Override
    public int hashCode() {
        return fifths * 31 + mode.hashCode();
    }

    @Override
    public String toString() {
        return tonicName() + " " + mode.displayName();
    }

    private static KeySignature fromTonic(String tonic, KeyMode mode) {
        int index = TONICS_BY_FIFTHS.indexOf(capitalised(tonic));
        int fifths = index - indexOffset(mode);
        if (index < 0 || Math.abs(fifths) > MOST_ACCIDENTALS) {
            throw new IllegalArgumentException("No " + mode.displayName() + " key signature for tonic: " + tonic);
        }
        return new KeySignature(fifths, mode);
    }

    private static int majorTonicPitchClass(int fifths) {
        return Math.floorMod(fifths * SEMITONES_PER_FIFTH, SEMITONES_PER_OCTAVE);
    }

    private static int indexOffset(KeyMode mode) {
        return mode == KeyMode.MAJOR ? MAJOR_TONIC_INDEX_OFFSET : MINOR_TONIC_INDEX_OFFSET;
    }

    private static String capitalised(String tonic) {
        return tonic.isEmpty() ? tonic : Character.toUpperCase(tonic.charAt(0)) + tonic.substring(1);
    }
}
