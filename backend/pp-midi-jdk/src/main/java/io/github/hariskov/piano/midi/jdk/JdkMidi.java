package io.github.hariskov.piano.midi.jdk;

import io.github.hariskov.piano.midi.input.MidiInputPort;
import io.github.hariskov.piano.midi.input.TimestampSource;

public final class JdkMidi {

    private JdkMidi() {
    }

    public static MidiInputPort inputPort() {
        return inputPort(TimestampSource.system());
    }

    public static MidiInputPort inputPort(TimestampSource clock) {
        return new JdkMidiInputPort(new JdkDeviceCatalog(new SystemMidiDevices()), clock);
    }
}
