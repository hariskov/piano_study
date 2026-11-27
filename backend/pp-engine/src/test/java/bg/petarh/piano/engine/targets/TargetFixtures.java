package bg.petarh.piano.engine.targets;

import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;

import java.util.List;

final class TargetFixtures {

    static final Fraction QUARTER = Fraction.of(1, 4);

    private TargetFixtures() {
    }

    static NoteEvent note(String pitch, Fraction start) {
        return new NoteEvent(Pitch.parse(pitch), start, QUARTER, Hand.RIGHT);
    }

    static Target target(String pitch, Fraction start) {
        return Target.of(List.of(note(pitch, start)));
    }
}
