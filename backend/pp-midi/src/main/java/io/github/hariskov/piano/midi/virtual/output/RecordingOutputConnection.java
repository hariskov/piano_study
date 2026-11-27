package io.github.hariskov.piano.midi.virtual.output;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.output.OutputConnection;
import io.github.hariskov.piano.midi.output.OutputEvent;

import java.util.function.Consumer;

final class RecordingOutputConnection implements OutputConnection {

    private final DeviceDescriptor device;
    private final Consumer<OutputEvent> recorder;
    private volatile boolean open = true;

    RecordingOutputConnection(DeviceDescriptor device, Consumer<OutputEvent> recorder) {
        this.device = device;
        this.recorder = recorder;
    }

    @Override
    public DeviceDescriptor device() {
        return device;
    }

    @Override
    public void send(OutputEvent event) {
        if (!open) {
            throw new IllegalStateException("Output connection is closed");
        }
        recorder.accept(event);
    }

    @Override
    public void close() {
        open = false;
    }
}
