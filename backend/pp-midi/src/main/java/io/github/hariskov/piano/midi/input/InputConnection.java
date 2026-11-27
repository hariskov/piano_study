package io.github.hariskov.piano.midi.input;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;

public interface InputConnection extends AutoCloseable {

    DeviceDescriptor device();

    @Override
    void close();
}
