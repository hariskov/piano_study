package io.github.hariskov.piano.midi.jdk;

import io.github.hariskov.piano.midi.input.InputListener;
import io.github.hariskov.piano.midi.input.TimestampSource;
import io.github.hariskov.piano.midi.jdk.message.InputMessageDecoder;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

final class DecodingReceiver implements Receiver {

    private final InputMessageDecoder decoder;
    private final TimestampSource clock;
    private final InputListener listener;
    private volatile boolean open = true;

    DecodingReceiver(InputMessageDecoder decoder, TimestampSource clock, InputListener listener) {
        this.decoder = decoder;
        this.clock = clock;
        this.listener = listener;
    }

    @Override
    public void send(MidiMessage message, long deviceTimestamp) {
        if (open) {
            long arrival = clock.nanos();
            decoder.decode(message, arrival).ifPresent(listener::onEvent);
        }
    }

    @Override
    public void close() {
        open = false;
    }
}
