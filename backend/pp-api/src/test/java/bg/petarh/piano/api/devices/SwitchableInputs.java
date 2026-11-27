package bg.petarh.piano.api.devices;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.midi.input.InputListener;
import bg.petarh.piano.midi.input.MidiInputPort;

import java.util.List;

public final class SwitchableInputs implements MidiInputPort {

    private final MidiInputPort port;
    private volatile boolean plugged = true;

    public SwitchableInputs(MidiInputPort port) {
        this.port = port;
    }

    public void unplug() {
        plugged = false;
    }

    public void plugIn() {
        plugged = true;
    }

    @Override
    public List<DeviceDescriptor> devices() {
        return plugged ? port.devices() : List.of();
    }

    @Override
    public InputConnection open(DeviceId id, InputListener listener) {
        if (!plugged) {
            throw new DeviceUnavailableException(id);
        }
        return port.open(id, listener);
    }
}
