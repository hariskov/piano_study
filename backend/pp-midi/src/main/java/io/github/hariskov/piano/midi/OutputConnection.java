package io.github.hariskov.piano.midi;

public interface OutputConnection extends AutoCloseable {

    DeviceDescriptor device();

    void send(OutputEvent event);

    @Override
    void close();
}
