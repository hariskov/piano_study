package io.github.hariskov.piano.midi;

import java.util.List;

public interface MidiInputPort {

    List<DeviceDescriptor> devices();

    InputConnection open(DeviceId id, InputListener listener);
}
