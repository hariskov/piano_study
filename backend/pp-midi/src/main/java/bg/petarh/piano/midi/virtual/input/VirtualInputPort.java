package bg.petarh.piano.midi.virtual.input;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.input.InputListener;
import bg.petarh.piano.midi.input.MidiInputPort;

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
