package bg.petarh.piano.model.keyboard;

import bg.petarh.piano.model.pitch.Pitch;

import java.util.Arrays;
import java.util.Optional;

public enum KeyboardPreset {
    KEYS_88("A0", "C8"),
    KEYS_76("E1", "G7"),
    KEYS_73("E1", "E7"),
    KEYS_61("C2", "C7"),
    KEYS_49("C2", "C6"),
    KEYS_37("C3", "C6"),
    KEYS_25("C3", "C5");

    private final KeyboardRange range;

    KeyboardPreset(String lowest, String highest) {
        this.range = KeyboardRange.of(Pitch.parse(lowest), Pitch.parse(highest));
    }

    public static Optional<KeyboardPreset> matching(KeyboardRange range) {
        return Arrays.stream(values()).filter(preset -> preset.range.equals(range)).findFirst();
    }

    public KeyboardRange range() {
        return range;
    }

    public int keyCount() {
        return range.keyCount();
    }
}
