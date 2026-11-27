package io.github.hariskov.piano.midi.jdk;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.device.DeviceId;
import io.github.hariskov.piano.midi.device.DeviceUnavailableException;
import io.github.hariskov.piano.midi.input.InputConnection;
import io.github.hariskov.piano.midi.input.InputListener;
import io.github.hariskov.piano.midi.input.MidiInputPort;
import io.github.hariskov.piano.midi.input.TimestampSource;
import io.github.hariskov.piano.midi.jdk.message.InputMessageDecoder;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;
import java.util.List;

final class JdkMidiInputPort implements MidiInputPort {

    private final JdkDeviceCatalog catalog;
    private final TimestampSource clock;
    private final InputMessageDecoder decoder = new InputMessageDecoder();

    JdkMidiInputPort(JdkDeviceCatalog catalog, TimestampSource clock) {
        this.catalog = catalog;
        this.clock = clock;
    }

    @Override
    public List<DeviceDescriptor> devices() {
        return catalog.inputs();
    }

    @Override
    public InputConnection open(DeviceId id, InputListener listener) {
        JdkDeviceCatalog.Entry entry = catalog.findInput(id).orElseThrow(() -> new DeviceUnavailableException(id));
        MidiDevice device = entry.device();
        try {
            device.open();
            Transmitter transmitter = device.getTransmitter();
            DecodingReceiver receiver = new DecodingReceiver(decoder, clock, listener);
            transmitter.setReceiver(receiver);
            return new JdkInputConnection(entry.descriptor(), device, transmitter, receiver);
        } catch (MidiUnavailableException e) {
            device.close();
            throw new DeviceUnavailableException(id, e);
        }
    }
}
