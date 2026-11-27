package io.github.hariskov.piano.model;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public record KeyboardRange(Pitch lowest, Pitch highest) {

    public KeyboardRange {
        if (lowest.compareTo(highest) > 0) {
            throw new IllegalArgumentException("Lowest key " + lowest + " is above highest key " + highest);
        }
    }

    public static KeyboardRange of(Pitch lowest, Pitch highest) {
        return new KeyboardRange(lowest, highest);
    }

    public static KeyboardRange spanning(Pitch first, Pitch second) {
        return first.compareTo(second) <= 0 ? new KeyboardRange(first, second) : new KeyboardRange(second, first);
    }

    public static KeyboardRange fullPiano() {
        return KeyboardPreset.KEYS_88.range();
    }

    public int keyCount() {
        return highest.midiNumber() - lowest.midiNumber() + 1;
    }

    public boolean contains(Pitch pitch) {
        return pitch.compareTo(lowest) >= 0 && pitch.compareTo(highest) <= 0;
    }

    public Optional<KeyboardRange> intersect(KeyboardRange other) {
        Pitch low = lowest.compareTo(other.lowest) >= 0 ? lowest : other.lowest;
        Pitch high = highest.compareTo(other.highest) <= 0 ? highest : other.highest;
        return low.compareTo(high) <= 0 ? Optional.of(new KeyboardRange(low, high)) : Optional.empty();
    }

    public List<Pitch> pitches() {
        return IntStream.rangeClosed(lowest.midiNumber(), highest.midiNumber())
                .mapToObj(Pitch::of)
                .toList();
    }
}
