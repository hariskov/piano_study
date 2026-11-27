package bg.petarh.piano.app.monitor;

import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputEventFormatterTest {

    private static final long START = 1_000_000_000L;

    private final InputEventFormatter formatter = new InputEventFormatter(START);

    @Test
    void formatsNoteOnWithElapsedTimeNoteNameAndVelocity() {
        InputEvent event = new InputEvent.NoteOn(Pitch.parse("C#4"), Velocity.of(100), START + 1_234_567_000L);

        assertThat(formatter.format(event)).isEqualTo("    1.235 s  NOTE ON   C#4  vel 100");
    }

    @Test
    void formatsNoteOff() {
        InputEvent event = new InputEvent.NoteOff(Pitch.parse("A0"), START + 61_000_000_000L);

        assertThat(formatter.format(event)).isEqualTo("   61.000 s  NOTE OFF  A0");
    }

    @Test
    void formatsSustainPedal() {
        assertThat(formatter.format(new InputEvent.SustainPedal(true, START)))
                .isEqualTo("    0.000 s  PEDAL     down");
        assertThat(formatter.format(new InputEvent.SustainPedal(false, START)))
                .isEqualTo("    0.000 s  PEDAL     up");
    }

    @Test
    void alignsVelocityForShortAndLongNoteNames() {
        String natural = formatter.format(new InputEvent.NoteOn(Pitch.parse("C8"), Velocity.of(7), START));
        String sharp = formatter.format(new InputEvent.NoteOn(Pitch.parse("F#3"), Velocity.of(127), START));

        assertThat(natural.indexOf("vel")).isEqualTo(sharp.indexOf("vel"));
        assertThat(natural).endsWith("vel   7");
    }
}
