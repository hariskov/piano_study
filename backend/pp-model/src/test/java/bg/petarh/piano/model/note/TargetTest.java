package bg.petarh.piano.model.note;

import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TargetTest {

    private static final Fraction QUARTER = Fraction.of(1, 4);

    @Test
    void singleNoteIsNotAChord() {
        Target target = Target.of(List.of(note("E4", Fraction.ZERO, Hand.RIGHT)));

        assertThat(target.isChord()).isFalse();
        assertThat(target.start()).isEqualTo(Fraction.ZERO);
        assertThat(target.pitches()).extracting(Pitch::name).containsExactly("E4");
    }

    @Test
    void listsChordPitchesFromLowToHigh() {
        Target target = Target.of(List.of(
                note("G4", Fraction.ZERO, Hand.RIGHT),
                note("C4", Fraction.ZERO, Hand.RIGHT),
                note("E4", Fraction.ZERO, Hand.RIGHT)));

        assertThat(target.isChord()).isTrue();
        assertThat(target.pitches()).extracting(Pitch::name).containsExactly("C4", "E4", "G4");
    }

    @Test
    void separatesPitchesByHand() {
        Target target = Target.of(List.of(
                note("C3", Fraction.ZERO, Hand.LEFT),
                note("E4", Fraction.ZERO, Hand.RIGHT),
                note("G4", Fraction.ZERO, Hand.RIGHT)));

        assertThat(target.pitchesFor(Hand.RIGHT)).extracting(Pitch::name).containsExactly("E4", "G4");
        assertThat(target.pitchesFor(Hand.LEFT)).extracting(Pitch::name).containsExactly("C3");
        assertThat(target.hands()).containsExactlyInAnyOrder(Hand.RIGHT, Hand.LEFT);
    }

    @Test
    void samePitchInBothHandsIsPlayedOnce() {
        Target target = Target.of(List.of(
                note("C4", Fraction.ZERO, Hand.LEFT),
                note("C4", Fraction.ZERO, Hand.RIGHT)));

        assertThat(target.pitches()).extracting(Pitch::name).containsExactly("C4");
        assertThat(target.isChord()).isFalse();
    }

    @Test
    void lastsAsLongAsItsLongestNote() {
        Target target = Target.of(List.of(
                new NoteEvent(Pitch.parse("C4"), Fraction.ZERO, QUARTER, Hand.RIGHT),
                new NoteEvent(Pitch.parse("C3"), Fraction.ZERO, Fraction.of(1, 2), Hand.LEFT)));

        assertThat(target.duration()).isEqualTo(Fraction.of(1, 2));
    }

    @Test
    void keepsItsOwnCopyOfTheNotes() {
        List<NoteEvent> notes = new ArrayList<>(List.of(note("C4", Fraction.ZERO, Hand.RIGHT)));
        Target target = Target.of(notes);

        notes.add(note("E4", Fraction.ZERO, Hand.RIGHT));

        assertThat(target.notes()).hasSize(1);
    }

    @Test
    void rejectsEmptyTarget() {
        assertThatIllegalArgumentException().isThrownBy(() -> Target.of(List.of()));
    }

    @Test
    void rejectsNotesStartingAtDifferentTimes() {
        assertThatIllegalArgumentException().isThrownBy(() -> Target.of(List.of(
                note("C4", Fraction.ZERO, Hand.RIGHT),
                note("D4", QUARTER, Hand.RIGHT))));
    }

    private static NoteEvent note(String pitch, Fraction start, Hand hand) {
        return new NoteEvent(Pitch.parse(pitch), start, QUARTER, hand);
    }
}
