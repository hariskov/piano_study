package bg.petarh.piano.model.chord;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class InversionTest {

    @ParameterizedTest
    @CsvSource({
            "0, ROOT_POSITION",
            "1, FIRST",
            "2, SECOND",
            "3, THIRD"
    })
    void isFoundByTheNumberOfNotesMovedUp(int number, Inversion inversion) {
        assertThat(Inversion.of(number)).isEqualTo(inversion);
        assertThat(inversion.number()).isEqualTo(number);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 4})
    void rejectsUnknownInversion(int number) {
        assertThatIllegalArgumentException().isThrownBy(() -> Inversion.of(number));
    }
}
