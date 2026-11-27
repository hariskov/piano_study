package io.github.hariskov.piano.midi.output;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;

public interface OutputConnection extends AutoCloseable {

    DeviceDescriptor device();

    void send(OutputEvent event);

    @Override
    void close();
}
