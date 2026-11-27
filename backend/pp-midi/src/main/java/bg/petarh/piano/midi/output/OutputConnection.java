package bg.petarh.piano.midi.output;

import bg.petarh.piano.midi.device.DeviceDescriptor;

public interface OutputConnection extends AutoCloseable {

    DeviceDescriptor device();

    void send(OutputEvent event);

    @Override
    void close();
}
