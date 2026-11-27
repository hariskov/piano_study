package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardPreset;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.NoteValue;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RandomNotesSettingsTest {

    private static final KeyboardRange PIANO = KeyboardRange.fullPiano();

    @Test
    void limitsTheRangeToTheKeyboard() {
        RandomNotesSettings settings = settings(Set.of(Hand.RIGHT), range("C5", "C8"),
                KeyboardPreset.KEYS_61.range(), NoteValue.QUARTER, TimeSignature.COMMON_TIME);

        assertThat(settings.playableRange()).isEqualTo(range("C5", "C7"));
    }

    @Test
    void rejectsRangeOutsideTheKeyboard() {
        assertThatIllegalArgumentException().isThrownBy(() -> settings(Set.of(Hand.RIGHT), range("A0", "B1"),
                KeyboardPreset.KEYS_61.range(), NoteValue.QUARTER, TimeSignature.COMMON_TIME));
    }

    @Test
    void rejectsPracticeWithoutHands() {
        assertThatIllegalArgumentException().isThrownBy(() -> settings(Set.of(), range("C4", "C5"),
                PIANO, NoteValue.QUARTER, TimeSignature.COMMON_TIME));
    }

    @Test
    void rejectsNoteValueThatDoesNotFillTheMeasure() {
        assertThatIllegalArgumentException().isThrownBy(() -> settings(Set.of(Hand.RIGHT), range("C4", "C5"),
                PIANO, NoteValue.HALF, TimeSignature.parse("3/4")));
    }

    @Test
    void bothHandsNeedNotesOnEachSideOfMiddleC() {
        assertThatIllegalArgumentException().isThrownBy(() -> settings(Set.of(Hand.RIGHT, Hand.LEFT),
                range("C4", "C5"), PIANO, NoteValue.QUARTER, TimeSignature.COMMON_TIME));
    }

    @Test
    void rangeNeedsAtLeastTwoNotesOfTheKey() {
        assertThatIllegalArgumentException().isThrownBy(() -> new RandomNotesSettings(Set.of(Hand.RIGHT),
                range("C#4", "D#4"), PIANO, KeySignature.major("C"), NoteValue.QUARTER,
                TimeSignature.COMMON_TIME, Level.ONE));
    }

    @Test
    void keepsItsOwnCopyOfTheHands() {
        RandomNotesSettings settings = settings(Set.of(Hand.LEFT), range("C2", "C4"),
                PIANO, NoteValue.QUARTER, TimeSignature.COMMON_TIME);

        assertThat(settings.hands()).containsExactly(Hand.LEFT);
    }

    private static RandomNotesSettings settings(Set<Hand> hands, KeyboardRange range, KeyboardRange keyboard,
                                                NoteValue noteValue, TimeSignature meter) {
        return new RandomNotesSettings(hands, range, keyboard, KeySignature.major("C"), noteValue, meter, Level.ONE);
    }

    private static KeyboardRange range(String lowest, String highest) {
        return KeyboardRange.of(Pitch.parse(lowest), Pitch.parse(highest));
    }
}
