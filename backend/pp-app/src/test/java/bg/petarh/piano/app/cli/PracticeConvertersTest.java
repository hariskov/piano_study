package bg.petarh.piano.app.cli;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PracticeConvertersTest {

    @Test
    void readsMajorAndMinorKeys() {
        PracticeConverters.KeyConverter keys = new PracticeConverters.KeyConverter();

        assertThat(keys.convert("C")).isEqualTo(KeySignature.major("C"));
        assertThat(keys.convert("Bb")).isEqualTo(KeySignature.major("Bb"));
        assertThat(keys.convert("F#m")).isEqualTo(KeySignature.minor("F#"));
        assertThat(keys.convert("am")).isEqualTo(KeySignature.minor("A"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "H", "Cmaj"})
    void rejectsUnknownKeys(String key) {
        assertThatIllegalArgumentException().isThrownBy(() -> new PracticeConverters.KeyConverter().convert(key));
    }

    @Test
    void readsRanges() {
        PracticeConverters.RangeConverter ranges = new PracticeConverters.RangeConverter();

        assertThat(ranges.convert("C4-C6")).isEqualTo(range("C4", "C6"));
        assertThat(ranges.convert("C#3-Bb4")).isEqualTo(range("C#3", "Bb4"));
        assertThat(ranges.convert("C-1-G9")).isEqualTo(KeyboardRange.of(Pitch.of(0), Pitch.of(127)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "C4", "C4-", "C6-C4", "C4..C6"})
    void rejectsInvalidRanges(String range) {
        assertThatIllegalArgumentException().isThrownBy(() -> new PracticeConverters.RangeConverter().convert(range));
    }

    @Test
    void readsKeyboardsAsAPresetSizeOrARange() {
        PracticeConverters.KeyboardConverter keyboards = new PracticeConverters.KeyboardConverter();

        assertThat(keyboards.convert("88")).isEqualTo(range("A0", "C8"));
        assertThat(keyboards.convert("61")).isEqualTo(range("C2", "C7"));
        assertThat(keyboards.convert("F1-F7")).isEqualTo(range("F1", "F7"));
    }

    @Test
    void rejectsKeyboardSizesWithoutAPreset() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PracticeConverters.KeyboardConverter().convert("50"));
    }

    private static KeyboardRange range(String lowest, String highest) {
        return KeyboardRange.of(Pitch.parse(lowest), Pitch.parse(highest));
    }
}
