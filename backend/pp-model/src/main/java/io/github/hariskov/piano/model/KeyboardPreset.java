package io.github.hariskov.piano.model;

public enum KeyboardPreset {
    KEYS_88(88, "A0", "C8"),
    KEYS_76(76, "E1", "G7"),
    KEYS_73(73, "E1", "E7"),
    KEYS_61(61, "C2", "C7"),
    KEYS_49(49, "C2", "C6"),
    KEYS_37(37, "C3", "C6"),
    KEYS_25(25, "C3", "C5");

    private final int keyCount;
    private final KeyboardRange range;

    KeyboardPreset(int keyCount, String lowest, String highest) {
        this.keyCount = keyCount;
        this.range = KeyboardRange.of(Pitch.parse(lowest), Pitch.parse(highest));
    }

    public int keyCount() {
        return keyCount;
    }

    public KeyboardRange range() {
        return range;
    }
}
