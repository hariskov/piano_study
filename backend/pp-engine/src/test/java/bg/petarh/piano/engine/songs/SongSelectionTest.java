package bg.petarh.piano.engine.songs;

import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.score.ScoreNote;
import bg.petarh.piano.model.score.Spelling;
import bg.petarh.piano.model.score.Tie;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SongSelectionTest {

    private static final KeyboardRange PIANO = KeyboardRange.fullPiano();

    private final Score song = Score.builder("Song").length(Fraction.whole(2))
            .part(new Part("P1", "Piano", 2, List.of(
                    note("E5", 0, 1), note("G5", 1, 1), note("C3", 0, 2), note("G2", 1, 2))))
            .part(new Part("P2", "Flute", 1, List.of(note("C6", 0, 1))))
            .build();

    @Test
    void playsTheChosenHandsOfThePart() {
        SongSelection right = SongSelection.of(song, "P1", Set.of(Hand.RIGHT), PIANO, OutOfRange.SHIFT_OCTAVE);

        assertThat(right.played()).extracting(note -> note.pitch().name()).containsExactly("E5", "G5");
        assertThat(SongSelection.of(song, "P1", Set.of(Hand.RIGHT, Hand.LEFT), PIANO, OutOfRange.SHIFT_OCTAVE).played())
                .hasSize(4);
    }

    @Test
    void leavesEverythingElseForTheAccompaniment() {
        SongSelection left = SongSelection.of(song, "P1", Set.of(Hand.LEFT), PIANO, OutOfRange.SHIFT_OCTAVE);

        assertThat(left.accompaniment()).extracting(note -> note.pitch().name()).containsExactlyInAnyOrder("E5", "G5", "C6");
        assertThat(left.played()).allMatch(note -> note.hand() == Hand.LEFT);
    }

    @Test
    void leavesTheOtherHandOutOfTheAccompanimentWhenAsked() {
        SongSelection left = SongSelection.of(song, "P1", Set.of(Hand.LEFT), PIANO, OutOfRange.SHIFT_OCTAVE, false);

        assertThat(left.accompaniment()).extracting(note -> note.pitch().name()).containsExactly("C6");
    }

    @Test
    void shiftsNotesOutsideTheKeyboardByOctaves() {
        KeyboardRange small = KeyboardRange.parse("C3-C5");

        SongSelection right = SongSelection.of(song, "P1", Set.of(Hand.RIGHT), small, OutOfRange.SHIFT_OCTAVE);

        assertThat(right.played()).extracting(note -> note.pitch().name()).containsExactly("E4", "G4");
        assertThat(right.shifted()).isEqualTo(2);
        assertThat(right.skipped()).isZero();
    }

    @Test
    void skipsNotesOutsideTheKeyboardWhenAsked() {
        KeyboardRange small = KeyboardRange.parse("C3-F5");

        SongSelection both = SongSelection.of(song, "P1", Set.of(Hand.RIGHT, Hand.LEFT), small, OutOfRange.SKIP);

        assertThat(both.played()).extracting(note -> note.pitch().name()).containsExactly("C3", "E5");
        assertThat(both.skipped()).isEqualTo(2);
    }

    @Test
    void skipsNotesThatNoOctaveFits() {
        KeyboardRange tiny = KeyboardRange.parse("C4-E4");

        SongSelection right = SongSelection.of(song, "P1", Set.of(Hand.RIGHT), tiny, OutOfRange.SHIFT_OCTAVE);

        assertThat(right.played()).extracting(note -> note.pitch().name()).containsExactly("E4");
        assertThat(right.skipped()).isEqualTo(1);
    }

    @Test
    void playsAOneStaffPartWithTheRightHand() {
        SongSelection flute = SongSelection.of(song, "P2", Set.of(Hand.RIGHT), PIANO, OutOfRange.SHIFT_OCTAVE);

        assertThat(flute.played()).extracting(NoteEvent::pitch).containsExactly(Pitch.parse("C6"));
    }

    @Test
    void refusesHandsWithoutNotesAndUnknownParts() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SongSelection.of(song, "P2", Set.of(Hand.LEFT), PIANO, OutOfRange.SKIP))
                .withMessageContaining("Flute");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SongSelection.of(song, "P9", Set.of(Hand.RIGHT), PIANO, OutOfRange.SKIP));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> SongSelection.of(song, "P1", Set.of(), PIANO, OutOfRange.SKIP));
    }

    private static ScoreNote note(String pitch, int quarter, int staff) {
        return new ScoreNote(Spelling.of(Pitch.parse(pitch), false), Fraction.of(quarter, 4), Fraction.of(1, 4), staff, 1,
                Tie.NONE);
    }
}
