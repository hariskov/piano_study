package io.github.hariskov.piano.midi.virtual.input;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.device.DeviceId;
import io.github.hariskov.piano.midi.input.InputEvent;
import io.github.hariskov.piano.midi.input.MidiInputPort;
import io.github.hariskov.piano.midi.input.TimestampSource;
import io.github.hariskov.piano.midi.value.Velocity;
import io.github.hariskov.piano.model.pitch.Pitch;

public final class VirtualKeyboard {

    private static final DeviceDescriptor DEVICE = new DeviceDescriptor(
            DeviceId.of("virtual:keyboard"), "Virtual keyboard", "On-screen and computer keyboard input");

    private final TimestampSource clock;
    private final VirtualInputPort port;

    private VirtualKeyboard(TimestampSource clock) {
        this.clock = clock;
        this.port = new VirtualInputPort(DEVICE);
    }

    public static VirtualKeyboard withClock(TimestampSource clock) {
        return new VirtualKeyboard(clock);
    }

    public DeviceDescriptor device() {
        return DEVICE;
    }

    public MidiInputPort port() {
        return port;
    }

    public void press(Pitch pitch, Velocity velocity) {
        port.publish(new InputEvent.NoteOn(pitch, velocity, clock.nanos()));
    }

    public void release(Pitch pitch) {
        port.publish(new InputEvent.NoteOff(pitch, clock.nanos()));
    }

    public void sustain(boolean down) {
        port.publish(new InputEvent.SustainPedal(down, clock.nanos()));
    }
}
