package bg.petarh.piano.model.chord;

import java.util.Arrays;
import java.util.List;

public enum ChordType {
    MAJOR("maj", "", 0, 4, 7),
    MINOR("min", "m", 0, 3, 7),
    DIMINISHED("dim", "dim", 0, 3, 6),
    AUGMENTED("aug", "aug", 0, 4, 8),
    MAJOR_SEVENTH("maj7", "maj7", 0, 4, 7, 11),
    MINOR_SEVENTH("min7", "m7", 0, 3, 7, 10),
    DOMINANT_SEVENTH("dom7", "7", 0, 4, 7, 10);

    private final String code;
    private final String symbol;
    private final List<Integer> semitonesAboveRoot;

    ChordType(String code, String symbol, Integer... semitonesAboveRoot) {
        this.code = code;
        this.symbol = symbol;
        this.semitonesAboveRoot = List.of(semitonesAboveRoot);
    }

    public static ChordType forCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown chord type: " + code));
    }

    public String code() {
        return code;
    }

    public String symbol() {
        return symbol;
    }

    public List<Integer> semitonesAboveRoot() {
        return semitonesAboveRoot;
    }

    public int noteCount() {
        return semitonesAboveRoot.size();
    }

    public boolean allows(Inversion inversion) {
        return inversion.number() < noteCount();
    }
}
