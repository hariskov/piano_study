package bg.petarh.piano.model.note;

import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TargetsTest {

    private static final Fraction QUARTER = Fraction.of(1, 4);

    @Test
    void groupsNotesStartingTogetherInTimeOrder() {
        List<Target> targets = Targets.from(List.of(
                note("D4", QUARTER, Hand.RIGHT),
                note("C4", Fraction.ZERO, Hand.RIGHT),
                note("C3", Fraction.ZERO, Hand.LEFT),
                note("E4", Fraction.of(1, 2), Hand.RIGHT)));

        assertThat(targets).extracting(Target::start)
                .containsExactly(Fraction.ZERO, QUARTER, Fraction.of(1, 2));
        assertThat(targets.getFirst().pitches()).extracting(Pitch::name).containsExactly("C3", "C4");
    }

    @Test
    void keepsOnlyTheHandsBeingPractised() {
        List<Target> targets = Targets.from(List.of(
                note("C4", Fraction.ZERO, Hand.RIGHT),
                note("C3", Fraction.ZERO, Hand.LEFT),
                note("G2", QUARTER, Hand.LEFT)), Set.of(Hand.RIGHT));

        assertThat(targets).hasSize(1);
        assertThat(targets.getFirst().hands()).containsExactly(Hand.RIGHT);
    }

    @Test
    void bothHandsKeepEverything() {
        List<NoteEvent> notes = List.of(note("C4", Fraction.ZERO, Hand.RIGHT), note("C3", QUARTER, Hand.LEFT));

        assertThat(Targets.from(notes, EnumSet.allOf(Hand.class))).isEqualTo(Targets.from(notes));
    }

    @Test
    void noNotesMeansNoTargets() {
        assertThat(Targets.from(List.of())).isEmpty();
    }

    private static NoteEvent note(String pitch, Fraction start, Hand hand) {
        return new NoteEvent(Pitch.parse(pitch), start, QUARTER, hand);
    }
}
