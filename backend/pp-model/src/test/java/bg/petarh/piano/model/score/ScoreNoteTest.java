package bg.petarh.piano.model.score;

import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ScoreNoteTest {

    static ScoreNote note(String spelling, int startQuarters, int quarters, int staff) {
        return note(spelling, startQuarters, quarters, staff, Tie.NONE);
    }

    static ScoreNote note(String spelling, int startQuarters, int quarters, int staff, Tie tie) {
        Pitch pitch = Pitch.parse(spelling);
        return new ScoreNote(Spelling.of(pitch, spelling.contains("b")), Fraction.of(startQuarters, 4),
                Fraction.of(quarters, 4), staff, 1, tie);
    }

    @Test
    void endsAfterItsDuration() {
        assertThat(note("C4", 2, 1, 1).end()).isEqualTo(Fraction.of(3, 4));
    }

    @Test
    void rejectsImpossibleNotes() {
        Spelling c4 = Spelling.of('C', 0, 4);
        assertThatIllegalArgumentException().isThrownBy(() -> new ScoreNote(c4, Fraction.ZERO, Fraction.ZERO, 1, 1, Tie.NONE));
        assertThatIllegalArgumentException().isThrownBy(() -> new ScoreNote(c4, Fraction.of(-1, 4), Fraction.of(1, 4), 1, 1, Tie.NONE));
        assertThatIllegalArgumentException().isThrownBy(() -> new ScoreNote(c4, Fraction.ZERO, Fraction.of(1, 4), 3, 1, Tie.NONE));
        assertThatIllegalArgumentException().isThrownBy(() -> new ScoreNote(c4, Fraction.ZERO, Fraction.of(1, 4), 1, 0, Tie.NONE));
    }

    @Test
    void playsStaffOneWithTheRightHandAndStaffTwoWithTheLeft() {
        Part piano = new Part("P1", "Piano", 2, List.of(note("E4", 0, 1, 1), note("C3", 0, 1, 2)));

        assertThat(piano.playableNotes()).extracting(NoteEvent::hand).containsExactly(Hand.LEFT, Hand.RIGHT);
    }

    @Test
    void joinsTiedNotesIntoOneNoteToPlay() {
        Part part = new Part("P1", "Melody", 1, List.of(
                note("G4", 2, 2, 1, Tie.START),
                note("G4", 4, 2, 1, Tie.CONTINUE),
                note("G4", 6, 1, 1, Tie.STOP),
                note("A4", 7, 1, 1)));

        assertThat(part.playableNotes()).containsExactly(
                new NoteEvent(Pitch.parse("G4"), Fraction.of(1, 2), Fraction.of(5, 4), Hand.RIGHT),
                new NoteEvent(Pitch.parse("A4"), Fraction.of(7, 4), Fraction.of(1, 4), Hand.RIGHT));
    }

    @Test
    void keepsAStartedTieThatNeverStopsAsWritten() {
        Part part = new Part("P1", "Melody", 1, List.of(note("G4", 0, 1, 1, Tie.START), note("A4", 1, 1, 1)));

        assertThat(part.playableNotes()).extracting(NoteEvent::duration)
                .containsExactly(Fraction.of(1, 4), Fraction.of(1, 4));
    }

    @Test
    void sortsPlayableNotesByTimeThenPitch() {
        Part part = new Part("P1", "Piano", 2, List.of(note("C5", 1, 1, 1), note("E4", 0, 1, 1), note("C4", 0, 1, 1)));

        assertThat(part.playableNotes()).extracting(note -> note.pitch().name()).containsExactly("C4", "E4", "C5");
    }

    @Test
    void rejectsNotesOnStavesThePartDoesNotHave() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Part("P1", "Flute", 1, List.of(note("C4", 0, 1, 2))));
    }
}
