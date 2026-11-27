package io.github.hariskov.piano.midi;

import java.util.List;

public interface MidiOutputPort {

    List<DeviceDescriptor> devices();

    OutputConnection open(DeviceId id);
}
