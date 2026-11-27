package bg.petarh.piano.midi.jdk;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.jdk.message.OutputMessageEncoder;
import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.midi.output.OutputEvent;
import bg.petarh.piano.midi.value.MidiChannel;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import java.util.concurrent.atomic.AtomicBoolean;

final class JdkOutputConnection implements OutputConnection {

    private static final long IMMEDIATELY = -1;
    private static final int CHANNEL_COUNT = 16;

    private final DeviceDescriptor descriptor;
    private final MidiDevice device;
    private final Receiver receiver;
    private final OutputMessageEncoder encoder;
    private final AtomicBoolean open = new AtomicBoolean(true);

    JdkOutputConnection(DeviceDescriptor descriptor, MidiDevice device, Receiver receiver, OutputMessageEncoder encoder) {
        this.descriptor = descriptor;
        this.device = device;
        this.receiver = receiver;
        this.encoder = encoder;
    }

    @Override
    public DeviceDescriptor device() {
        return descriptor;
    }

    @Override
    public void send(OutputEvent event) {
        if (!open.get()) {
            throw new IllegalStateException("Output connection is closed");
        }
        transmit(event);
    }

    @Override
    public void close() {
        if (!open.compareAndSet(true, false)) {
            return;
        }
        try (device; receiver) {
            silenceAllChannels();
        }
    }

    private void silenceAllChannels() {
        for (int channel = 0; channel < CHANNEL_COUNT; channel++) {
            transmit(new OutputEvent.AllNotesOff(MidiChannel.of(channel)));
        }
    }

    private void transmit(OutputEvent event) {
        receiver.send(encoder.encode(event), IMMEDIATELY);
    }
}
