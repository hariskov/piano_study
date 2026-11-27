package io.github.hariskov.piano.midi;

public interface InputConnection extends AutoCloseable {

    DeviceDescriptor device();

    @Override
    void close();
}
