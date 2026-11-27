package io.github.hariskov.piano.midi.virtual.output;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.device.DeviceId;
import io.github.hariskov.piano.midi.output.MidiOutputPort;
import io.github.hariskov.piano.midi.output.OutputEvent;

import java.util.List;

public final class SilentOutput {

    private static final DeviceDescriptor DEVICE = new DeviceDescriptor(
            DeviceId.of("virtual:silent"), "Silent output", "Plays nothing, records every sent event");

    private final RecordingOutputPort port = new RecordingOutputPort(DEVICE);

    private SilentOutput() {
    }

    public static SilentOutput create() {
        return new SilentOutput();
    }

    public DeviceDescriptor device() {
        return DEVICE;
    }

    public MidiOutputPort port() {
        return port;
    }

    public List<OutputEvent> sentEvents() {
        return port.sentEvents();
    }
}
