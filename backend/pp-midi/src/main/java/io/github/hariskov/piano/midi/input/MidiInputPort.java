package io.github.hariskov.piano.midi.input;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.device.DeviceId;

import java.util.List;

public interface MidiInputPort {

    List<DeviceDescriptor> devices();

    InputConnection open(DeviceId id, InputListener listener);
}
