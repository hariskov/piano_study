package bg.petarh.piano.generator.chords;

import bg.petarh.piano.model.chord.ChordType;
import bg.petarh.piano.model.chord.Inversion;
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

class ChordSettingsTest {

    private static final Set<ChordType> TRIADS = Set.of(ChordType.MAJOR, ChordType.MINOR);
    private static final Set<Inversion> ROOT_ONLY = Set.of(Inversion.ROOT_POSITION);

    @Test
    void limitsTheRangeToTheKeyboard() {
        ChordSettings settings = settings(Set.of(Hand.RIGHT), range("C5", "C8"), KeyboardPreset.KEYS_61.range(),
                TRIADS, ROOT_ONLY, NoteValue.HALF);

        assertThat(settings.playableRange()).isEqualTo(range("C5", "C7"));
    }

    @Test
    void rejectsRangeOutsideTheKeyboard() {
        assertThatIllegalArgumentException().isThrownBy(() -> settings(Set.of(Hand.RIGHT), range("A0", "B1"),
                KeyboardPreset.KEYS_61.range(), TRIADS, ROOT_ONLY, NoteValue.HALF));
    }

    @Test
    void rejectsPracticeWithoutHands() {
        assertThatIllegalArgumentException().isThrownBy(() -> settings(Set.of(), range("C4", "C6"),
                KeyboardRange.fullPiano(), TRIADS, ROOT_ONLY, NoteValue.HALF));
    }

    @Test
    void rejectsPracticeWithoutChordTypes() {
        assertThatIllegalArgumentException().isThrownBy(() -> settings(Set.of(Hand.RIGHT), range("C4", "C6"),
                KeyboardRange.fullPiano(), Set.of(), ROOT_ONLY, NoteValue.HALF));
    }

    @Test
    void rejectsPracticeWithoutInversions() {
        assertThatIllegalArgumentException().isThrownBy(() -> settings(Set.of(Hand.RIGHT), range("C4", "C6"),
                KeyboardRange.fullPiano(), TRIADS, Set.of(), NoteValue.HALF));
    }

    @Test
    void rejectsNoteValueThatDoesNotFillTheMeasure() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ChordSettings(Set.of(Hand.RIGHT),
                range("C4", "C6"), KeyboardRange.fullPiano(), KeySignature.major("C"), TRIADS, ROOT_ONLY,
                NoteValue.HALF, TimeSignature.parse("3/4")));
    }

    @Test
    void rejectsRangeTooSmallForAnyChord() {
        assertThatIllegalArgumentException().isThrownBy(() -> settings(Set.of(Hand.RIGHT), range("C4", "F4"),
                KeyboardRange.fullPiano(), TRIADS, ROOT_ONLY, NoteValue.HALF));
    }

    @Test
    void rejectsThirdInversionWhenOnlyTriadsAreChosen() {
        assertThatIllegalArgumentException().isThrownBy(() -> settings(Set.of(Hand.RIGHT), range("C4", "C6"),
                KeyboardRange.fullPiano(), TRIADS, Set.of(Inversion.THIRD), NoteValue.HALF));
    }

    @Test
    void bothHandsNeedRoomForAChordOnEachSideOfMiddleC() {
        assertThatIllegalArgumentException().isThrownBy(() -> settings(Set.of(Hand.RIGHT, Hand.LEFT),
                range("A3", "C6"), KeyboardRange.fullPiano(), TRIADS, ROOT_ONLY, NoteValue.HALF));
    }

    private static ChordSettings settings(Set<Hand> hands, KeyboardRange range, KeyboardRange keyboard,
                                          Set<ChordType> types, Set<Inversion> inversions, NoteValue noteValue) {
        return new ChordSettings(hands, range, keyboard, KeySignature.major("C"), types, inversions, noteValue,
                TimeSignature.COMMON_TIME);
    }

    private static KeyboardRange range(String lowest, String highest) {
        return KeyboardRange.of(Pitch.parse(lowest), Pitch.parse(highest));
    }
}
