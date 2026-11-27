package bg.petarh.piano.app.practice;

import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TargetNamesTest {

    @Test
    void namesASingleNote() {
        assertThat(TargetNames.describe(target(note("E4", Hand.RIGHT)))).isEqualTo("E4");
    }

    @Test
    void namesNotesTogetherFromLowToHigh() {
        assertThat(TargetNames.describe(target(note("G4", Hand.RIGHT), note("C4", Hand.RIGHT))))
                .isEqualTo("C4 G4");
    }

    @Test
    void namesEachHandWhenBothPlay() {
        assertThat(TargetNames.describe(target(note("C3", Hand.LEFT), note("E4", Hand.RIGHT), note("G4", Hand.RIGHT))))
                .isEqualTo("R: E4 G4  L: C3");
    }

    private static Target target(NoteEvent... notes) {
        return Target.of(List.of(notes));
    }

    private static NoteEvent note(String pitch, Hand hand) {
        return new NoteEvent(Pitch.parse(pitch), Fraction.ZERO, Fraction.of(1, 4), hand);
    }
}
