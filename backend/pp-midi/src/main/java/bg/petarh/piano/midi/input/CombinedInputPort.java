package bg.petarh.piano.midi.input;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;

import java.util.List;

final class CombinedInputPort implements MidiInputPort {

    private final List<MidiInputPort> ports;

    CombinedInputPort(List<MidiInputPort> ports) {
        this.ports = List.copyOf(ports);
    }

    @Override
    public List<DeviceDescriptor> devices() {
        return ports.stream().flatMap(port -> port.devices().stream()).toList();
    }

    @Override
    public InputConnection open(DeviceId id, InputListener listener) {
        return ports.stream()
                .filter(port -> port.devices().stream().anyMatch(device -> device.id().equals(id)))
                .findFirst()
                .orElseThrow(() -> new DeviceUnavailableException(id))
                .open(id, listener);
    }
}
