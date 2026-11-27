package bg.petarh.piano.midi.input;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.device.DeviceId;

import java.util.List;

public interface MidiInputPort {

    List<DeviceDescriptor> devices();

    InputConnection open(DeviceId id, InputListener listener);
}
