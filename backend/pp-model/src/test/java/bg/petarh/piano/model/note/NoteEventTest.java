package bg.petarh.piano.model.note;

import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class NoteEventTest {

    private static final Pitch C4 = Pitch.parse("C4");
    private static final Fraction QUARTER = Fraction.of(1, 4);

    @Test
    void endsAfterItsDuration() {
        NoteEvent note = new NoteEvent(C4, Fraction.of(1, 2), QUARTER, Hand.RIGHT);

        assertThat(note.end()).isEqualTo(Fraction.of(3, 4));
    }

    @Test
    void canStartAtTheBeginning() {
        assertThat(new NoteEvent(C4, Fraction.ZERO, QUARTER, Hand.LEFT).start()).isEqualTo(Fraction.ZERO);
    }

    @Test
    void rejectsStartBeforeTheBeginning() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new NoteEvent(C4, Fraction.of(-1, 4), QUARTER, Hand.RIGHT));
    }

    @Test
    void rejectsDurationThatIsNotPositive() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new NoteEvent(C4, Fraction.ZERO, Fraction.ZERO, Hand.RIGHT));
    }

    @Test
    void rejectsMissingValues() {
        assertThatNullPointerException().isThrownBy(() -> new NoteEvent(null, Fraction.ZERO, QUARTER, Hand.RIGHT));
        assertThatNullPointerException().isThrownBy(() -> new NoteEvent(C4, null, QUARTER, Hand.RIGHT));
        assertThatNullPointerException().isThrownBy(() -> new NoteEvent(C4, Fraction.ZERO, null, Hand.RIGHT));
        assertThatNullPointerException().isThrownBy(() -> new NoteEvent(C4, Fraction.ZERO, QUARTER, null));
    }
}
