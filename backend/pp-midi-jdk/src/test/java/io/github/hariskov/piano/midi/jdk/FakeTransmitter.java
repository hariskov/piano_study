package io.github.hariskov.piano.midi.jdk;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.Transmitter;

final class FakeTransmitter implements Transmitter {

    private Receiver receiver;
    private boolean closed;

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
    }
}
