package bg.petarh.piano.api.session;

import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LatencyOffsetTest {

    private static final Pitch C4 = Pitch.parse("C4");

    @Test
    void movesEveryEventEarlierByTheOffset() {
        LatencyOffset offset = LatencyOffset.ofMillis(20);

        assertThat(offset.apply(new InputEvent.NoteOn(C4, Velocity.of(64), 100_000_000L)))
                .isEqualTo(new InputEvent.NoteOn(C4, Velocity.of(64), 80_000_000L));
        assertThat(offset.apply(new InputEvent.NoteOff(C4, 100_000_000L)))
                .isEqualTo(new InputEvent.NoteOff(C4, 80_000_000L));
        assertThat(offset.apply(new InputEvent.SustainPedal(true, 100_000_000L)))
                .isEqualTo(new InputEvent.SustainPedal(true, 80_000_000L));
    }

    @Test
    void negativeOffsetMovesEventsLater() {
        assertThat(LatencyOffset.ofMillis(-5).apply(new InputEvent.NoteOff(C4, 0)))
                .isEqualTo(new InputEvent.NoteOff(C4, 5_000_000L));
    }
}
