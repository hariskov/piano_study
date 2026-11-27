package io.github.hariskov.piano.midi.jdk.message;

import io.github.hariskov.piano.midi.input.InputEvent;
import io.github.hariskov.piano.midi.value.Velocity;
import io.github.hariskov.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import static org.assertj.core.api.Assertions.assertThat;

class InputMessageDecoderTest {

    private static final long ARRIVAL = 42_000L;
    private static final Pitch MIDDLE_C = Pitch.of(60);

    private final InputMessageDecoder decoder = new InputMessageDecoder();

    @Test
    void decodesNoteOn() throws InvalidMidiDataException {
        MidiMessage message = new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 100);

        assertThat(decoder.decode(message, ARRIVAL))
                .contains(new InputEvent.NoteOn(MIDDLE_C, Velocity.of(100), ARRIVAL));
    }

    @Test
    void decodesNoteOnWithZeroVelocityAsNoteOff() throws InvalidMidiDataException {
        MidiMessage message = new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 0);

        assertThat(decoder.decode(message, ARRIVAL)).contains(new InputEvent.NoteOff(MIDDLE_C, ARRIVAL));
    }

    @Test
    void decodesNoteOff() throws InvalidMidiDataException {
        MidiMessage message = new ShortMessage(ShortMessage.NOTE_OFF, 0, 60, 64);

        assertThat(decoder.decode(message, ARRIVAL)).contains(new InputEvent.NoteOff(MIDDLE_C, ARRIVAL));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9, 15})
    void decodesNotesFromAnyChannel(int channel) throws InvalidMidiDataException {
        MidiMessage message = new ShortMessage(ShortMessage.NOTE_ON, channel, 60, 100);

        assertThat(decoder.decode(message, ARRIVAL)).isPresent();
    }

    @ParameterizedTest
    @ValueSource(ints = {64, 127})
    void decodesSustainPedalDown(int value) throws InvalidMidiDataException {
        MidiMessage message = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 64, value);

        assertThat(decoder.decode(message, ARRIVAL)).contains(new InputEvent.SustainPedal(true, ARRIVAL));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 63})
    void decodesSustainPedalUp(int value) throws InvalidMidiDataException {
        MidiMessage message = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 64, value);

        assertThat(decoder.decode(message, ARRIVAL)).contains(new InputEvent.SustainPedal(false, ARRIVAL));
    }

    @Test
    void ignoresOtherControllers() throws InvalidMidiDataException {
        MidiMessage modulation = new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 1, 127);

        assertThat(decoder.decode(modulation, ARRIVAL)).isEmpty();
    }

    @Test
    void ignoresSystemRealTimeMessages() throws InvalidMidiDataException {
        MidiMessage activeSensing = new ShortMessage(ShortMessage.ACTIVE_SENSING);
        MidiMessage clock = new ShortMessage(ShortMessage.TIMING_CLOCK);

        assertThat(decoder.decode(activeSensing, ARRIVAL)).isEmpty();
        assertThat(decoder.decode(clock, ARRIVAL)).isEmpty();
    }

    @Test
    void ignoresOtherChannelMessages() throws InvalidMidiDataException {
        MidiMessage programChange = new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, 5, 0);
        MidiMessage pitchBend = new ShortMessage(ShortMessage.PITCH_BEND, 0, 0, 64);

        assertThat(decoder.decode(programChange, ARRIVAL)).isEmpty();
        assertThat(decoder.decode(pitchBend, ARRIVAL)).isEmpty();
    }

    @Test
    void ignoresSysexAndMetaMessages() throws InvalidMidiDataException {
        byte[] identityRequest = {(byte) 0xF0, 0x7E, 0x7F, 0x06, 0x01, (byte) 0xF7};
        MidiMessage sysex = new SysexMessage(identityRequest, identityRequest.length);
        MidiMessage meta = new MetaMessage(0x51, new byte[]{0x07, (byte) 0xA1, 0x20}, 3);

        assertThat(decoder.decode(sysex, ARRIVAL)).isEmpty();
        assertThat(decoder.decode(meta, ARRIVAL)).isEmpty();
    }
}
