package io.github.hariskov.piano.midi;

import io.github.hariskov.piano.model.pitch.Pitch;

public sealed interface InputEvent {

    long timestampNanos();

    record NoteOn(Pitch pitch, int velocity, long timestampNanos) implements InputEvent {
        public NoteOn {
            MidiRanges.requireAudibleVelocity(velocity);
        }
    }

    record NoteOff(Pitch pitch, long timestampNanos) implements InputEvent {
    }

    record SustainPedal(boolean down, long timestampNanos) implements InputEvent {
    }
}
