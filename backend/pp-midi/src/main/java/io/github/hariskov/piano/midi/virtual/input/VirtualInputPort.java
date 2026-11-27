package io.github.hariskov.piano.midi.virtual.input;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.device.DeviceId;
import io.github.hariskov.piano.midi.device.DeviceUnavailableException;
import io.github.hariskov.piano.midi.input.InputConnection;
import io.github.hariskov.piano.midi.input.InputEvent;
import io.github.hariskov.piano.midi.input.InputListener;
import io.github.hariskov.piano.midi.input.MidiInputPort;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

final class VirtualInputPort implements MidiInputPort {

    private final DeviceDescriptor device;
    private final Set<VirtualInputConnection> connections = new CopyOnWriteArraySet<>();

    VirtualInputPort(DeviceDescriptor device) {
        this.device = device;
    }

    @Override
    public List<DeviceDescriptor> devices() {
        return List.of(device);
    }

    @Override
    public InputConnection open(DeviceId id, InputListener listener) {
        if (!device.id().equals(id)) {
            throw new DeviceUnavailableException(id);
        }
        VirtualInputConnection connection = new VirtualInputConnection(device, listener, connections::remove);
        connections.add(connection);
        return connection;
    }

    void publish(InputEvent event) {
        connections.forEach(connection -> connection.deliver(event));
    }
}
