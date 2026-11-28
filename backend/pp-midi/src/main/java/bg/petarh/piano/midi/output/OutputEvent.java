package bg.petarh.piano.midi.output;

import bg.petarh.piano.midi.value.Instrument;
import bg.petarh.piano.midi.value.MidiChannel;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;

public sealed interface OutputEvent {

    MidiChannel channel();

    record NoteOn(Pitch pitch, Velocity velocity, MidiChannel channel) implements OutputEvent {
    }

    record NoteOff(Pitch pitch, MidiChannel channel) implements OutputEvent {
    }

    record SustainPedal(boolean down, MidiChannel channel) implements OutputEvent {
    }

    record AllNotesOff(MidiChannel channel) implements OutputEvent {
    }

    record ProgramChange(Instrument instrument, MidiChannel channel) implements OutputEvent {
    }
}
