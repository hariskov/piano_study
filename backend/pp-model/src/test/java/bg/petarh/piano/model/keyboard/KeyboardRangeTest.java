package bg.petarh.piano.model.keyboard;

import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyboardRangeTest {

    @ParameterizedTest
    @CsvSource({
            "KEYS_88, A0, C8, 88",
            "KEYS_76, E1, G7, 76",
            "KEYS_73, E1, E7, 73",
            "KEYS_61, C2, C7, 61",
            "KEYS_49, C2, C6, 49",
            "KEYS_37, C3, C6, 37",
            "KEYS_25, C3, C5, 25"
    })
    void presetsCoverStandardKeyboardSizes(KeyboardPreset preset, String lowest, String highest, int keyCount) {
        KeyboardRange range = preset.range();

        assertThat(range.lowest()).isEqualTo(Pitch.parse(lowest));
        assertThat(range.highest()).isEqualTo(Pitch.parse(highest));
        assertThat(range.keyCount()).isEqualTo(keyCount);
        assertThat(preset.keyCount()).isEqualTo(keyCount);
    }

    @Test
    void defaultsToFullPiano() {
        assertThat(KeyboardRange.fullPiano()).isEqualTo(KeyboardPreset.KEYS_88.range());
    }

    @Test
    void acceptsCustomRange() {
        KeyboardRange range = KeyboardRange.of(Pitch.parse("F1"), Pitch.parse("F7"));

        assertThat(range.keyCount()).isEqualTo(73);
    }

    @Test
    void acceptsSingleKeyRange() {
        KeyboardRange range = KeyboardRange.of(Pitch.parse("C4"), Pitch.parse("C4"));

        assertThat(range.keyCount()).isEqualTo(1);
    }

    @Test
    void rejectsRangeWhoseLowestIsAboveHighest() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> KeyboardRange.of(Pitch.parse("C5"), Pitch.parse("C4")));
    }

    @Test
    void containsPitchesWithinBoundsInclusive() {
        KeyboardRange range = KeyboardPreset.KEYS_61.range();

        assertThat(range.contains(Pitch.parse("C2"))).isTrue();
        assertThat(range.contains(Pitch.parse("C7"))).isTrue();
        assertThat(range.contains(Pitch.parse("B1"))).isFalse();
        assertThat(range.contains(Pitch.parse("C#7"))).isFalse();
    }

    @Test
    void intersectsOverlappingRanges() {
        KeyboardRange keyboard = KeyboardPreset.KEYS_61.range();
        KeyboardRange requested = KeyboardRange.of(Pitch.parse("A0"), Pitch.parse("C4"));

        assertThat(keyboard.intersect(requested))
                .contains(KeyboardRange.of(Pitch.parse("C2"), Pitch.parse("C4")));
    }

    @Test
    void intersectionOfDisjointRangesIsEmpty() {
        KeyboardRange low = KeyboardRange.of(Pitch.parse("A0"), Pitch.parse("B1"));
        KeyboardRange high = KeyboardRange.of(Pitch.parse("C2"), Pitch.parse("C3"));

        assertThat(low.intersect(high)).isEmpty();
    }

    @Test
    void detectsRangeFromTwoPressedKeysInAnyOrder() {
        KeyboardRange range = KeyboardRange.spanning(Pitch.parse("C8"), Pitch.parse("A0"));

        assertThat(range).isEqualTo(KeyboardRange.fullPiano());
    }

    @Test
    void listsPitchesFromLowestToHighest() {
        KeyboardRange range = KeyboardRange.of(Pitch.parse("C4"), Pitch.parse("E4"));

        assertThat(range.pitches()).containsExactly(
                Pitch.parse("C4"), Pitch.parse("C#4"), Pitch.parse("D4"), Pitch.parse("D#4"), Pitch.parse("E4"));
    }

    @Test
    void exposesPitchesAsReadOnlyList() {
        KeyboardRange range = KeyboardRange.of(Pitch.parse("C4"), Pitch.parse("E4"));

        assertThatThrownBy(() -> range.pitches().add(Pitch.parse("F4")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rangesWithSameBoundsAreEqual() {
        KeyboardRange first = KeyboardRange.of(Pitch.parse("C2"), Pitch.parse("C7"));

        assertThat(first).isEqualTo(KeyboardPreset.KEYS_61.range());
        assertThat(first).hasSameHashCodeAs(KeyboardPreset.KEYS_61.range());
        assertThat(first).hasToString("C2-C7");
    }

    @ParameterizedTest
    @CsvSource({
            "A0, C8, KEYS_88",
            "E1, G7, KEYS_76",
            "C2, C7, KEYS_61",
            "C3, C5, KEYS_25"
    })
    void findsPresetMatchingARange(String lowest, String highest, KeyboardPreset expected) {
        KeyboardRange range = KeyboardRange.of(Pitch.parse(lowest), Pitch.parse(highest));

        assertThat(KeyboardPreset.matching(range)).contains(expected);
    }

    @Test
    void customRangeMatchesNoPreset() {
        KeyboardRange range = KeyboardRange.of(Pitch.parse("F1"), Pitch.parse("F7"));

        assertThat(KeyboardPreset.matching(range)).isEmpty();
    }
}
