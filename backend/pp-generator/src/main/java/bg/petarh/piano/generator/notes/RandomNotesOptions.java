package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.chord.ChordType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record RandomNotesOptions(boolean allowAccidentals, boolean mixedNoteValues, Set<ChordType> chordTypes) {

    private static final RandomNotesOptions DEFAULTS =
            new RandomNotesOptions(false, false, EnumSet.of(ChordType.MAJOR, ChordType.MINOR));

    public RandomNotesOptions {
        if (chordTypes.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one chord type");
        }
        chordTypes = Collections.unmodifiableSet(EnumSet.copyOf(chordTypes));
    }

    public static RandomNotesOptions defaults() {
        return DEFAULTS;
    }

    public RandomNotesOptions withAccidentals(boolean allowed) {
        return new RandomNotesOptions(allowed, mixedNoteValues, chordTypes);
    }

    public RandomNotesOptions withMixedNoteValues(boolean mixed) {
        return new RandomNotesOptions(allowAccidentals, mixed, chordTypes);
    }

    public RandomNotesOptions withChordTypes(Set<ChordType> types) {
        return new RandomNotesOptions(allowAccidentals, mixedNoteValues, types);
    }
}
