package bg.petarh.piano.musicxml;

import bg.petarh.piano.model.time.Fraction;

import java.util.List;
import java.util.Optional;

record NoteType(String name, int dots, boolean triplet) {

    private static final List<NoteType> PLAIN = List.of(
            new NoteType("whole", 0, false), new NoteType("half", 0, false), new NoteType("quarter", 0, false),
            new NoteType("eighth", 0, false), new NoteType("16th", 0, false), new NoteType("32nd", 0, false),
            new NoteType("64th", 0, false));
    private static final Fraction WHOLE = Fraction.whole(1);

    static Optional<NoteType> of(Fraction duration) {
        for (int index = 0; index < PLAIN.size(); index++) {
            Fraction plain = WHOLE.dividedBy(Fraction.whole(1L << index));
            String name = PLAIN.get(index).name();
            if (duration.equals(plain)) {
                return Optional.of(new NoteType(name, 0, false));
            }
            if (duration.equals(plain.times(Fraction.of(3, 2)))) {
                return Optional.of(new NoteType(name, 1, false));
            }
            if (duration.equals(plain.times(Fraction.of(7, 4)))) {
                return Optional.of(new NoteType(name, 2, false));
            }
            if (duration.equals(plain.times(Fraction.of(2, 3)))) {
                return Optional.of(new NoteType(name, 0, true));
            }
        }
        return Optional.empty();
    }
}
