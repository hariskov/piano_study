package bg.petarh.piano.model.keyboard;

import bg.petarh.piano.model.pitch.Pitch;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public final class KeyboardRange {

    private static final Pattern RANGE = Pattern.compile("([A-Ga-g][#b]?-?\\d)-([A-Ga-g][#b]?-?\\d)");

    private final Pitch lowest;
    private final Pitch highest;

    private KeyboardRange(Pitch lowest, Pitch highest) {
        this.lowest = lowest;
        this.highest = highest;
    }

    public static KeyboardRange of(Pitch lowest, Pitch highest) {
        if (lowest.compareTo(highest) > 0) {
            throw new IllegalArgumentException("Lowest key " + lowest + " is above highest key " + highest);
        }
        return new KeyboardRange(lowest, highest);
    }

    public static KeyboardRange parse(String text) {
        Matcher matcher = RANGE.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Expected a range like C4-C6: " + text);
        }
        return of(Pitch.parse(matcher.group(1)), Pitch.parse(matcher.group(2)));
    }

    public static KeyboardRange parseKeyboard(String text) {
        if (text.isEmpty() || !text.chars().allMatch(Character::isDigit)) {
            return parse(text);
        }
        int keys = Integer.parseInt(text);
        return KeyboardPreset.withKeys(keys)
                .map(KeyboardPreset::range)
                .orElseThrow(() -> new IllegalArgumentException("No " + keys + "-key preset; give a range like F1-F7"));
    }

    public static KeyboardRange spanning(Pitch first, Pitch second) {
        return first.compareTo(second) <= 0 ? new KeyboardRange(first, second) : new KeyboardRange(second, first);
    }

    public static KeyboardRange fullPiano() {
        return KeyboardPreset.KEYS_88.range();
    }

    public Pitch lowest() {
        return lowest;
    }

    public Pitch highest() {
        return highest;
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

    @Override
    public boolean equals(Object other) {
        return other instanceof KeyboardRange range && range.lowest.equals(lowest) && range.highest.equals(highest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowest, highest);
    }

    @Override
    public String toString() {
        return lowest + "-" + highest;
    }
}
