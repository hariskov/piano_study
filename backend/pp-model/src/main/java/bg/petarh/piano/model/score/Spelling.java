package bg.petarh.piano.model.score;

import bg.petarh.piano.model.pitch.Pitch;

public record Spelling(char step, int alter, int octave) {

    private static final String STEPS = "CDEFGAB";
    private static final int[] NATURAL_PITCH_CLASSES = {0, 2, 4, 5, 7, 9, 11};
    private static final String[] SHARP_SPELLINGS = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final String[] FLAT_SPELLINGS = {"C", "Db", "D", "Eb", "E", "F", "Gb", "G", "Ab", "A", "Bb", "B"};
    private static final int MOST_ALTERATION = 2;
    private static final int SEMITONES_PER_OCTAVE = 12;

    public Spelling {
        if (STEPS.indexOf(step) < 0) {
            throw new IllegalArgumentException("Step must be a note letter A-G: " + step);
        }
        if (Math.abs(alter) > MOST_ALTERATION) {
            throw new IllegalArgumentException("At most two sharps or flats: " + alter);
        }
    }

    public static Spelling of(char step, int alter, int octave) {
        return new Spelling(step, alter, octave);
    }

    public static Spelling of(Pitch pitch, boolean preferFlats) {
        String name = (preferFlats ? FLAT_SPELLINGS : SHARP_SPELLINGS)[pitch.pitchClass()];
        int alter = name.length() == 1 ? 0 : name.charAt(1) == '#' ? 1 : -1;
        return new Spelling(name.charAt(0), alter, pitch.octave());
    }

    public Pitch pitch() {
        int natural = NATURAL_PITCH_CLASSES[STEPS.indexOf(step)];
        return Pitch.of((octave + 1) * SEMITONES_PER_OCTAVE + natural + alter);
    }

    @Override
    public String toString() {
        return step + (alter < 0 ? "b".repeat(-alter) : "#".repeat(alter)) + octave;
    }
}
