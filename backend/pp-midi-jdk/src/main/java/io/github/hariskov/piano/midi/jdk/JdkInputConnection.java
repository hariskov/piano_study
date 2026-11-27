package io.github.hariskov.piano.midi.jdk;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.input.InputConnection;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

final class JdkInputConnection implements InputConnection {

    private final DeviceDescriptor descriptor;
    private final MidiDevice device;
    private final Transmitter transmitter;
    private final Receiver receiver;

    JdkInputConnection(DeviceDescriptor descriptor, MidiDevice device, Transmitter transmitter, Receiver receiver) {
        this.descriptor = descriptor;
        this.device = device;
        this.transmitter = transmitter;
        this.receiver = receiver;
    }

    @Override
    public DeviceDescriptor device() {
        return descriptor;
    }

    @Override
    public void close() {
        try (device; transmitter; receiver) {
            // Resources close in reverse order: receiver, transmitter, then device, even if one of them fails.
        }
    }
}
