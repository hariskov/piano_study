package io.github.hariskov.piano.midi.virtual;

import io.github.hariskov.piano.midi.DeviceDescriptor;
import io.github.hariskov.piano.midi.DeviceId;
import io.github.hariskov.piano.midi.DeviceUnavailableException;
import io.github.hariskov.piano.midi.InputConnection;
import io.github.hariskov.piano.midi.InputEvent;
import io.github.hariskov.piano.midi.InputListener;
import io.github.hariskov.piano.midi.MidiInputPort;
import io.github.hariskov.piano.midi.TimestampSource;
import io.github.hariskov.piano.model.pitch.Pitch;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class VirtualInputPort implements MidiInputPort {

    public static final DeviceId DEVICE_ID = new DeviceId("virtual:keyboard");
    private static final DeviceDescriptor DEVICE =
            new DeviceDescriptor(DEVICE_ID, "Virtual keyboard", "On-screen and computer keyboard input");

    private final TimestampSource clock;
    private final List<InputListener> listeners = new CopyOnWriteArrayList<>();

    public VirtualInputPort(TimestampSource clock) {
        this.clock = clock;
    }

    @Override
    public List<DeviceDescriptor> devices() {
        return List.of(DEVICE);
    }

    @Override
    public InputConnection open(DeviceId id, InputListener listener) {
        if (!DEVICE_ID.equals(id)) {
            throw new DeviceUnavailableException(id);
        }
        listeners.add(listener);
        return new InputConnection() {
            @Override
            public DeviceDescriptor device() {
                return DEVICE;
            }

            @Override
            public void close() {
                listeners.remove(listener);
            }
        };
    }

    public void press(Pitch pitch, int velocity) {
        publish(new InputEvent.NoteOn(pitch, velocity, clock.nanos()));
    }

    public void release(Pitch pitch) {
        publish(new InputEvent.NoteOff(pitch, clock.nanos()));
    }

    public void sustain(boolean down) {
        publish(new InputEvent.SustainPedal(down, clock.nanos()));
    }

    private void publish(InputEvent event) {
        listeners.forEach(listener -> listener.onEvent(event));
    }
}
