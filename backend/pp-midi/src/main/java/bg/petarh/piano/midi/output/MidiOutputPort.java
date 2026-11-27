package bg.petarh.piano.midi.output;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.device.DeviceId;

import java.util.List;

public interface MidiOutputPort {

    List<DeviceDescriptor> devices();

    OutputConnection open(DeviceId id);
}
