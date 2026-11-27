package bg.petarh.piano.app.cli;

import bg.petarh.piano.model.chord.ChordType;
import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PracticeConvertersTest {

    @Test
    void readsOptionValuesWithTheModelParsers() {
        assertThat(new PracticeConverters.KeyConverter().convert("F#m")).isEqualTo(KeySignature.minor("F#"));
        assertThat(new PracticeConverters.RangeConverter().convert("C4-C6")).isEqualTo(KeyboardRange.parse("C4-C6"));
        assertThat(new PracticeConverters.KeyboardConverter().convert("88")).isEqualTo(KeyboardRange.fullPiano());
        assertThat(new PracticeConverters.TimeSignatureConverter().convert("6/8")).isEqualTo(TimeSignature.parse("6/8"));
        assertThat(new PracticeConverters.ChordTypeConverter().convert("dom7")).isEqualTo(ChordType.DOMINANT_SEVENTH);
    }
}
