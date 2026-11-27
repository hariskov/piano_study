package io.github.hariskov.piano.midi.output;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.device.DeviceId;

import java.util.List;

public interface MidiOutputPort {

    List<DeviceDescriptor> devices();

    OutputConnection open(DeviceId id);
}
