package bg.petarh.piano.app.cli;

import bg.petarh.piano.app.lifetime.Lifetime;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.input.TimestampSource;
import bg.petarh.piano.midi.output.MidiOutputPort;

import java.io.InputStream;
import java.io.OutputStream;

public record CliEnvironment(MidiInputPort inputs, MidiOutputPort outputs, TimestampSource clock, Lifetime lifetime,
                             InputStream stdin, OutputStream stdout) {

    public CliEnvironment(MidiInputPort inputs, MidiOutputPort outputs, TimestampSource clock, Lifetime lifetime) {
        this(inputs, outputs, clock, lifetime, System.in, System.out);
    }
}
