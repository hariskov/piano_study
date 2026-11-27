package io.github.hariskov.piano.midi.jdk;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.util.ArrayList;
import java.util.List;

final class FakeReceiver implements Receiver {

    private final boolean failOnClose;
    private final List<ShortMessage> received = new ArrayList<>();
    private boolean closed;

    FakeReceiver(boolean failOnClose) {
        this.failOnClose = failOnClose;
    }

    List<ShortMessage> received() {
        return List.copyOf(received);
    }

    boolean isClosed() {
        return closed;
    }

    @Override
    public void send(MidiMessage message, long timeStamp) {
        received.add((ShortMessage) message);
    }

    @Override
    public void close() {
        closed = true;
        if (failOnClose) {
            throw new IllegalStateException("receiver close failed");
        }
    }
}
