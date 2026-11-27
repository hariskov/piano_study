package bg.petarh.piano.midi.virtual.input;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.input.InputListener;

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
