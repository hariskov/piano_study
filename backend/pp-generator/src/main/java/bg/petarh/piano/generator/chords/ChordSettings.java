package bg.petarh.piano.generator.chords;

import bg.petarh.piano.model.chord.ChordType;
import bg.petarh.piano.model.chord.Inversion;
import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.time.NoteValue;
import bg.petarh.piano.model.time.TimeSignature;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record ChordSettings(Set<Hand> hands, KeyboardRange range, KeyboardRange keyboard, KeySignature key,
                            Set<ChordType> types, Set<Inversion> inversions, NoteValue noteValue,
                            TimeSignature meter) {

    public ChordSettings {
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(keyboard, "keyboard");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(noteValue, "noteValue");
        Objects.requireNonNull(meter, "meter");
        hands = nonEmptyCopy(hands, "hand");
        types = nonEmptyCopy(types, "chord type");
        inversions = nonEmptyCopy(inversions, "inversion");
        if (range.intersect(keyboard).isEmpty()) {
            throw new IllegalArgumentException("Range " + range + " is outside the keyboard " + keyboard);
        }
        if (!noteValue.fillsMeasureOf(meter)) {
            throw new IllegalArgumentException(noteValue + " notes don't fill a " + meter + " measure");
        }
        KeyboardRange playable = range.intersect(keyboard).orElseThrow();
        for (Hand hand : hands) {
            ChordCandidates.forHand(playable, key, hands, types, inversions, hand);
        }
    }

    public KeyboardRange playableRange() {
        return range.intersect(keyboard).orElseThrow();
    }

    private static <E extends Enum<E>> Set<E> nonEmptyCopy(Set<E> values, String what) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one " + what);
        }
        return Collections.unmodifiableSet(EnumSet.copyOf(values));
    }
}
