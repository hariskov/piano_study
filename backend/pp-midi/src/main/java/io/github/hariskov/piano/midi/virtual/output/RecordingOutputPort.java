package io.github.hariskov.piano.midi.virtual.output;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.device.DeviceId;
import io.github.hariskov.piano.midi.device.DeviceUnavailableException;
import io.github.hariskov.piano.midi.output.MidiOutputPort;
import io.github.hariskov.piano.midi.output.OutputConnection;
import io.github.hariskov.piano.midi.output.OutputEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class RecordingOutputPort implements MidiOutputPort {

    private final DeviceDescriptor device;
    private final List<OutputEvent> sent = new CopyOnWriteArrayList<>();

    RecordingOutputPort(DeviceDescriptor device) {
        this.device = device;
    }

    @Override
    public List<DeviceDescriptor> devices() {
        return List.of(device);
    }

    @Override
    public OutputConnection open(DeviceId id) {
        if (!device.id().equals(id)) {
            throw new DeviceUnavailableException(id);
        }
        return new RecordingOutputConnection(device, sent::add);
    }

    List<OutputEvent> sentEvents() {
        return List.copyOf(sent);
    }
}
