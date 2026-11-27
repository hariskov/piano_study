package bg.petarh.piano.midi.virtual.input;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.input.TimestampSource;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;

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
