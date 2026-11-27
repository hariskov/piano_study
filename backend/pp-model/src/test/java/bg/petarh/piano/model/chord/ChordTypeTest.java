package bg.petarh.piano.model.chord;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ChordTypeTest {

    @ParameterizedTest
    @CsvSource({
            "maj, MAJOR, 3",
            "min, MINOR, 3",
            "dim, DIMINISHED, 3",
            "aug, AUGMENTED, 3",
            "maj7, MAJOR_SEVENTH, 4",
            "min7, MINOR_SEVENTH, 4",
            "dom7, DOMINANT_SEVENTH, 4"
    })
    void isFoundByItsCode(String code, ChordType type, int noteCount) {
        assertThat(ChordType.forCode(code)).isEqualTo(type);
        assertThat(type.code()).isEqualTo(code);
        assertThat(type.noteCount()).isEqualTo(noteCount);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sus4", "MAJ"})
    void rejectsUnknownCode(String code) {
        assertThatIllegalArgumentException().isThrownBy(() -> ChordType.forCode(code));
    }
}
