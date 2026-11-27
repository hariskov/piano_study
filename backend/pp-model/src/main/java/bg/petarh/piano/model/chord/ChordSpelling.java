package bg.petarh.piano.model.chord;

final class ChordSpelling {

    private static final String LETTERS = "CDEFGAB";
    private static final int[] NATURAL_PITCH_CLASSES = {0, 2, 4, 5, 7, 9, 11};
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final int LETTERS_PER_CHORD_STEP = 2;
    private static final int TRITONE = 6;

    private final int rootLetter;
    private final int rootPitchClass;

    private ChordSpelling(int rootLetter, int rootPitchClass) {
        this.rootLetter = rootLetter;
        this.rootPitchClass = rootPitchClass;
    }

    static ChordSpelling forRoot(int rootPitchClass, boolean preferFlats) {
        int natural = naturalLetterAtOrBelow(rootPitchClass);
        boolean isNatural = NATURAL_PITCH_CLASSES[natural] == rootPitchClass;
        int letter = isNatural || !preferFlats ? natural : (natural + 1) % LETTERS.length();
        return new ChordSpelling(letter, rootPitchClass);
    }

    String toneName(int chordStep, int semitonesAboveRoot) {
        int letter = (rootLetter + chordStep * LETTERS_PER_CHORD_STEP) % LETTERS.length();
        int pitchClass = (rootPitchClass + semitonesAboveRoot) % SEMITONES_PER_OCTAVE;
        int alteration = Math.floorMod(pitchClass - NATURAL_PITCH_CLASSES[letter] + TRITONE, SEMITONES_PER_OCTAVE) - TRITONE;
        return LETTERS.charAt(letter) + accidental(alteration);
    }

    private static int naturalLetterAtOrBelow(int pitchClass) {
        int letter = NATURAL_PITCH_CLASSES.length - 1;
        while (NATURAL_PITCH_CLASSES[letter] > pitchClass) {
            letter--;
        }
        return letter;
    }

    private static String accidental(int alteration) {
        return alteration < 0 ? "b".repeat(-alteration) : "#".repeat(alteration);
    }
}
