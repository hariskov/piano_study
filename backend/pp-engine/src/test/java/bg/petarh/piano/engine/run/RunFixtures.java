package bg.petarh.piano.engine.run;

import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;

import java.util.List;

final class RunFixtures {

    static final Fraction QUARTER = Fraction.of(1, 4);

    private RunFixtures() {
    }

    static Target target(String pitch, Fraction start) {
        return Target.of(List.of(new NoteEvent(Pitch.parse(pitch), start, QUARTER, Hand.RIGHT)));
    }

    static InputEvent press(String pitch, long atNanos) {
        return new InputEvent.NoteOn(Pitch.parse(pitch), Velocity.of(64), atNanos);
    }
}
