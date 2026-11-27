package io.github.hariskov.piano.midi;

import io.github.hariskov.piano.model.Pitch;

public sealed interface OutputEvent {

    int channel();

    record NoteOn(Pitch pitch, int velocity, int channel) implements OutputEvent {
        public NoteOn {
            MidiRanges.requireAudibleVelocity(velocity);
            MidiRanges.requireChannel(channel);
        }
    }

    record NoteOff(Pitch pitch, int channel) implements OutputEvent {
        public NoteOff {
            MidiRanges.requireChannel(channel);
        }
    }

    record AllNotesOff(int channel) implements OutputEvent {
        public AllNotesOff {
            MidiRanges.requireChannel(channel);
        }
    }
}
