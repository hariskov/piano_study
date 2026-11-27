package bg.petarh.piano.midi.input;

import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;

public sealed interface InputEvent {

    long timestampNanos();

    record NoteOn(Pitch pitch, Velocity velocity, long timestampNanos) implements InputEvent {
    }

    record NoteOff(Pitch pitch, long timestampNanos) implements InputEvent {
    }

    record SustainPedal(boolean down, long timestampNanos) implements InputEvent {
    }
}
