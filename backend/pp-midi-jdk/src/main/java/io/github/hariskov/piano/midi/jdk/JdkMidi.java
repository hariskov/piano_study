package io.github.hariskov.piano.midi.jdk;

import io.github.hariskov.piano.midi.input.MidiInputPort;
import io.github.hariskov.piano.midi.input.TimestampSource;
import io.github.hariskov.piano.midi.output.MidiOutputPort;

public final class JdkMidi {

    private JdkMidi() {
    }

    public static MidiInputPort inputPort() {
        return inputPort(TimestampSource.system());
    }

    public static MidiInputPort inputPort(TimestampSource clock) {
        return new JdkMidiInputPort(systemCatalog(), clock);
    }

    public static MidiOutputPort outputPort() {
        return new JdkMidiOutputPort(systemCatalog());
    }

    private static JdkDeviceCatalog systemCatalog() {
        return new JdkDeviceCatalog(new SystemMidiDevices());
    }
}
