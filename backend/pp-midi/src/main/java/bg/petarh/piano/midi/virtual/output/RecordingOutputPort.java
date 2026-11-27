package bg.petarh.piano.midi.virtual.output;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.output.MidiOutputPort;
import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.midi.output.OutputEvent;

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
