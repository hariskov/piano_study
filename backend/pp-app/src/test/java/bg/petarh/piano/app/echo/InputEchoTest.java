package bg.petarh.piano.app.echo;

import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.output.OutputEvent;
import bg.petarh.piano.midi.value.MidiChannel;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputEchoTest {

    private static final Pitch MIDDLE_C = Pitch.parse("C4");
    private static final MidiChannel FIRST = MidiChannel.of(0);

    private final SilentOutput output = SilentOutput.create();
    private final InputEcho echo = new InputEcho(output.port().open(output.device().id()), FIRST);

    @Test
    void playsPressedKeysWithTheirVelocity() {
        echo.onEvent(new InputEvent.NoteOn(MIDDLE_C, Velocity.of(37), 0L));

        assertThat(output.sentEvents()).containsExactly(new OutputEvent.NoteOn(MIDDLE_C, Velocity.of(37), FIRST));
    }

    @Test
    void releasesKeys() {
        echo.onEvent(new InputEvent.NoteOff(MIDDLE_C, 0L));

        assertThat(output.sentEvents()).containsExactly(new OutputEvent.NoteOff(MIDDLE_C, FIRST));
    }

    @Test
    void forwardsSustainPedal() {
        echo.onEvent(new InputEvent.SustainPedal(true, 0L));
        echo.onEvent(new InputEvent.SustainPedal(false, 0L));

        assertThat(output.sentEvents()).containsExactly(
                new OutputEvent.SustainPedal(true, FIRST),
                new OutputEvent.SustainPedal(false, FIRST));
    }
}
