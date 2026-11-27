package io.github.hariskov.piano.midi.jdk;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;
import java.util.ArrayList;
import java.util.List;

final class FakeMidiDevice implements MidiDevice {

    private final Info info;
    private final int maxReceivers;
    private final int maxTransmitters;
    private final List<FakeTransmitter> transmitters = new ArrayList<>();
    private final List<FakeReceiver> receivers = new ArrayList<>();
    private boolean open;
    private boolean failOnOpen;
    private boolean failOnTransmitter;
    private boolean transmittersFailOnClose;
    private boolean failOnReceiver;
    private boolean receiversFailOnClose;

    private FakeMidiDevice(String name, String description, int maxReceivers, int maxTransmitters) {
        this.info = new Info(name, "Fake vendor", description, "1.0") {
        };
        this.maxReceivers = maxReceivers;
        this.maxTransmitters = maxTransmitters;
    }

    static FakeMidiDevice input(String name) {
        return new FakeMidiDevice(name, name + " input", 0, -1);
    }

    static FakeMidiDevice output(String name) {
        return new FakeMidiDevice(name, name + " output", -1, 0);
    }

    FakeMidiDevice failingOnOpen() {
        failOnOpen = true;
        return this;
    }

    FakeMidiDevice failingOnTransmitter() {
        failOnTransmitter = true;
        return this;
    }

    FakeMidiDevice withTransmittersFailingOnClose() {
        transmittersFailOnClose = true;
        return this;
    }

    FakeMidiDevice failingOnReceiver() {
        failOnReceiver = true;
        return this;
    }

    FakeMidiDevice withReceiversFailingOnClose() {
        receiversFailOnClose = true;
        return this;
    }

    FakeReceiver lastReceiver() {
        return receivers.getLast();
    }

    FakeTransmitter lastTransmitter() {
        return transmitters.getLast();
    }

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public void open() throws MidiUnavailableException {
        if (failOnOpen) {
            throw new MidiUnavailableException("busy");
        }
        open = true;
    }

    @Override
    public void close() {
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public long getMicrosecondPosition() {
        return -1;
    }

    @Override
    public int getMaxReceivers() {
        return maxReceivers;
    }

    @Override
    public int getMaxTransmitters() {
        return maxTransmitters;
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        if (maxReceivers == 0 || failOnReceiver) {
            throw new MidiUnavailableException("no receivers");
        }
        FakeReceiver receiver = new FakeReceiver(receiversFailOnClose);
        receivers.add(receiver);
        return receiver;
    }

    @Override
    public List<Receiver> getReceivers() {
        return List.copyOf(receivers);
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        if (maxTransmitters == 0 || failOnTransmitter) {
            throw new MidiUnavailableException("no transmitters");
        }
        FakeTransmitter transmitter = new FakeTransmitter(transmittersFailOnClose);
        transmitters.add(transmitter);
        return transmitter;
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return List.copyOf(transmitters);
    }
}
