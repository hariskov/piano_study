package bg.petarh.piano.midi.input;

import bg.petarh.piano.midi.device.DeviceDescriptor;

public interface InputConnection extends AutoCloseable {

    DeviceDescriptor device();

    @Override
    void close();
}
