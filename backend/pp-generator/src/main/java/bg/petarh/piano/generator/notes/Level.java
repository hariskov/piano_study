package bg.petarh.piano.generator.notes;

import java.util.Arrays;

public enum Level {
    ONE(1, 1),
    TWO(2, 1),
    THREE(3, 2),
    FOUR(4, 3);

    private static final int FIRST_LEVEL_WITH_MIXED_NOTE_VALUES = 3;
    private static final int FIRST_LEVEL_WITH_ACCIDENTALS = 4;

    private final int number;
    private final int mostNotesTogether;

    Level(int number, int mostNotesTogether) {
        this.number = number;
        this.mostNotesTogether = mostNotesTogether;
    }

    public int number() {
        return number;
    }

    public static Level of(int number) {
        return Arrays.stream(values())
                .filter(level -> level.number == number)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown level: " + number));
    }

    int mostNotesTogether() {
        return mostNotesTogether;
    }

    boolean mixesNoteValues() {
        return number >= FIRST_LEVEL_WITH_MIXED_NOTE_VALUES;
    }

    boolean playsAccidentals() {
        return number >= FIRST_LEVEL_WITH_ACCIDENTALS;
    }
}
