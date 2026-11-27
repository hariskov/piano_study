package io.github.hariskov.piano.midi.input;

import io.github.hariskov.piano.midi.value.Velocity;
import io.github.hariskov.piano.model.pitch.Pitch;

public sealed interface InputEvent {

    long timestampNanos();

    record NoteOn(Pitch pitch, Velocity velocity, long timestampNanos) implements InputEvent {
    }

    record NoteOff(Pitch pitch, long timestampNanos) implements InputEvent {
    }

    record SustainPedal(boolean down, long timestampNanos) implements InputEvent {
    }
}
