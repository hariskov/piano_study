package bg.petarh.piano.model.chord;

import java.util.Arrays;

public enum Inversion {
    ROOT_POSITION(0),
    FIRST(1),
    SECOND(2),
    THIRD(3);

    private final int number;

    Inversion(int number) {
        this.number = number;
    }

    public static Inversion of(int number) {
        return Arrays.stream(values())
                .filter(inversion -> inversion.number == number)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown inversion: " + number));
    }

    public int number() {
        return number;
    }
}
