package bg.petarh.piano.generator.notes;

import java.util.Arrays;

public enum Level {
    ONE(1),
    TWO(2);

    private final int number;

    Level(int number) {
        this.number = number;
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
}
