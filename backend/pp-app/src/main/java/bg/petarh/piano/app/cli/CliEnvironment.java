package bg.petarh.piano.app.cli;

import bg.petarh.piano.app.lifetime.Lifetime;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.input.TimestampSource;
import bg.petarh.piano.midi.output.MidiOutputPort;

public record CliEnvironment(MidiInputPort inputs, MidiOutputPort outputs, TimestampSource clock, Lifetime lifetime) {
}
