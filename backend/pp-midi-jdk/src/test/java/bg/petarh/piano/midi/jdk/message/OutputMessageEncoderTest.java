package bg.petarh.piano.midi.jdk.message;

import bg.petarh.piano.midi.output.OutputEvent;
import bg.petarh.piano.midi.value.Instrument;
import bg.petarh.piano.midi.value.MidiChannel;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;
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
    void encodesTheInstrumentAsProgramChange() {
        ShortMessage message = encoder.encode(new OutputEvent.ProgramChange(Instrument.of(56), CHANNEL_3));

        assertThat(message.getCommand()).isEqualTo(ShortMessage.PROGRAM_CHANGE);
        assertThat(message.getChannel()).isEqualTo(2);
        assertThat(message.getData1()).isEqualTo(56);
    }

    @Test
    void encodesAllNotesOffAsChannelModeMessage() {
        ShortMessage message = encoder.encode(new OutputEvent.AllNotesOff(CHANNEL_3));

        assertThat(message.getCommand()).isEqualTo(ShortMessage.CONTROL_CHANGE);
        assertThat(message.getChannel()).isEqualTo(2);
        assertThat(message.getData1()).isEqualTo(123);
        assertThat(message.getData2()).isZero();
    }

    @Test
    void encodesSustainPedalDownAsController64AtFullValue() {
        ShortMessage message = encoder.encode(new OutputEvent.SustainPedal(true, CHANNEL_3));

        assertThat(message.getCommand()).isEqualTo(ShortMessage.CONTROL_CHANGE);
        assertThat(message.getChannel()).isEqualTo(2);
        assertThat(message.getData1()).isEqualTo(64);
        assertThat(message.getData2()).isEqualTo(127);
    }

    @Test
    void encodesSustainPedalUpAsController64AtZero() {
        ShortMessage message = encoder.encode(new OutputEvent.SustainPedal(false, CHANNEL_3));

        assertThat(message.getData1()).isEqualTo(64);
        assertThat(message.getData2()).isZero();
    }
}
