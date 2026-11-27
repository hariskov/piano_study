package io.github.hariskov.piano.midi.output;

import io.github.hariskov.piano.midi.value.MidiChannel;
import io.github.hariskov.piano.midi.value.Velocity;
import io.github.hariskov.piano.model.pitch.Pitch;

public sealed interface OutputEvent {

    MidiChannel channel();

    record NoteOn(Pitch pitch, Velocity velocity, MidiChannel channel) implements OutputEvent {
    }

    record NoteOff(Pitch pitch, MidiChannel channel) implements OutputEvent {
    }

    record AllNotesOff(MidiChannel channel) implements OutputEvent {
    }
}
