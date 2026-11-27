package io.github.hariskov.piano.midi.jdk;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

final class FakeTransmitter implements Transmitter {

    private final boolean failOnClose;
    private Receiver receiver;
    private boolean closed;

    FakeTransmitter(boolean failOnClose) {
        this.failOnClose = failOnClose;
    }

    void emit(MidiMessage message) {
        if (!closed && receiver != null) {
            receiver.send(message, -1);
        }
    }

    boolean isClosed() {
        return closed;
    }

    @Override
    public void setReceiver(Receiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public Receiver getReceiver() {
        return receiver;
    }

    @Override
    public void close() {
        closed = true;
        if (failOnClose) {
            throw new IllegalStateException("transmitter close failed");
        }
    }
}
