package io.github.hariskov.piano.midi.virtual.input;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.input.InputConnection;
import io.github.hariskov.piano.midi.input.InputEvent;
import io.github.hariskov.piano.midi.input.InputListener;

import java.util.function.Consumer;

final class VirtualInputConnection implements InputConnection {

    private final DeviceDescriptor device;
    private final InputListener listener;
    private final Consumer<VirtualInputConnection> onClose;

    VirtualInputConnection(DeviceDescriptor device, InputListener listener, Consumer<VirtualInputConnection> onClose) {
        this.device = device;
        this.listener = listener;
        this.onClose = onClose;
    }

    @Override
    public DeviceDescriptor device() {
        return device;
    }

    @Override
    public void close() {
        onClose.accept(this);
    }

    void deliver(InputEvent event) {
        listener.onEvent(event);
    }
}
