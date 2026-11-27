package io.github.hariskov.piano.midi.virtual.output;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.device.DeviceId;
import io.github.hariskov.piano.midi.device.DeviceUnavailableException;
import io.github.hariskov.piano.midi.output.MidiOutputPort;
import io.github.hariskov.piano.midi.output.OutputConnection;
import io.github.hariskov.piano.midi.output.OutputEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class VirtualOutputPort implements MidiOutputPort {

    public static final DeviceId DEVICE_ID = DeviceId.of("virtual:silent");
    private static final DeviceDescriptor DEVICE =
            new DeviceDescriptor(DEVICE_ID, "Silent output", "Discards sound, keeps a record of sent events");

    private final List<OutputEvent> sent = new CopyOnWriteArrayList<>();

    public VirtualOutputPort() {
    }

    @Override
    public List<DeviceDescriptor> devices() {
        return List.of(DEVICE);
    }

    @Override
    public OutputConnection open(DeviceId id) {
        if (!DEVICE_ID.equals(id)) {
            throw new DeviceUnavailableException(id);
        }
        return new RecordingConnection();
    }

    public List<OutputEvent> sent() {
        return List.copyOf(sent);
    }

    private final class RecordingConnection implements OutputConnection {

        private volatile boolean open = true;

        @Override
        public DeviceDescriptor device() {
            return DEVICE;
        }

        @Override
        public void send(OutputEvent event) {
            if (!open) {
                throw new IllegalStateException("Output connection is closed");
            }
            sent.add(event);
        }

        @Override
        public void close() {
            open = false;
        }
    }
}
