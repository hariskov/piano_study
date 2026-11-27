package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.time.NoteValue;
import bg.petarh.piano.model.time.TimeSignature;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public record RandomNotesSettings(Set<Hand> hands, KeyboardRange range, KeyboardRange keyboard, KeySignature key,
                                  NoteValue noteValue, TimeSignature meter, Level level,
                                  RandomNotesOptions options) {

    public RandomNotesSettings {
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(keyboard, "keyboard");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(noteValue, "noteValue");
        Objects.requireNonNull(meter, "meter");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(options, "options");
        if (hands.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one hand");
        }
        hands = Collections.unmodifiableSet(EnumSet.copyOf(hands));
        if (range.intersect(keyboard).isEmpty()) {
            throw new IllegalArgumentException("Range " + range + " is outside the keyboard " + keyboard);
        }
        if (!noteValue.fillsMeasureOf(meter)) {
            throw new IllegalArgumentException(noteValue + " notes don't fill a " + meter + " measure");
        }
        KeyboardRange playable = range.intersect(keyboard).orElseThrow();
        for (Hand hand : hands) {
            HandPitches.forHand(playable, key, hands, hand);
        }
    }

    public RandomNotesSettings(Set<Hand> hands, KeyboardRange range, KeyboardRange keyboard, KeySignature key,
                               NoteValue noteValue, TimeSignature meter, Level level) {
        this(hands, range, keyboard, key, noteValue, meter, level, RandomNotesOptions.defaults());
    }

    public boolean playsAccidentals() {
        return options.allowAccidentals() && level.playsAccidentals();
    }

    public boolean playsMixedNoteValues() {
        return options.mixedNoteValues() && level.mixesNoteValues();
    }

    public KeyboardRange playableRange() {
        return range.intersect(keyboard).orElseThrow();
    }
}
