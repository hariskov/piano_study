package io.github.hariskov.piano.midi.jdk.message;

import io.github.hariskov.piano.midi.output.OutputEvent;
import io.github.hariskov.piano.midi.value.MidiChannel;
import io.github.hariskov.piano.midi.value.Velocity;
import io.github.hariskov.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import javax.sound.midi.ShortMessage;

import static org.assertj.core.api.Assertions.assertThat;

class OutputMessageEncoderTest {

    private static final Pitch A4 = Pitch.of(69);
    private static final MidiChannel CHANNEL_3 = MidiChannel.of(2);

    private final OutputMessageEncoder encoder = new OutputMessageEncoder();

    @Test
    void encodesNoteOn() {
        ShortMessage message = encoder.encode(new OutputEvent.NoteOn(A4, Velocity.of(90), CHANNEL_3));

        assertThat(message.getCommand()).isEqualTo(ShortMessage.NOTE_ON);
        assertThat(message.getChannel()).isEqualTo(2);
        assertThat(message.getData1()).isEqualTo(69);
        assertThat(message.getData2()).isEqualTo(90);
    }

    @Test
    void encodesNoteOff() {
        ShortMessage message = encoder.encode(new OutputEvent.NoteOff(A4, CHANNEL_3));

        assertThat(message.getCommand()).isEqualTo(ShortMessage.NOTE_OFF);
        assertThat(message.getChannel()).isEqualTo(2);
        assertThat(message.getData1()).isEqualTo(69);
        assertThat(message.getData2()).isZero();
    }

    @Test
    void encodesAllNotesOffAsChannelModeMessage() {
        ShortMessage message = encoder.encode(new OutputEvent.AllNotesOff(CHANNEL_3));

        assertThat(message.getCommand()).isEqualTo(ShortMessage.CONTROL_CHANGE);
        assertThat(message.getChannel()).isEqualTo(2);
        assertThat(message.getData1()).isEqualTo(123);
        assertThat(message.getData2()).isZero();
    }
}
