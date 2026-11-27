package bg.petarh.piano.model.score;

import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SpellingTest {

    @ParameterizedTest
    @CsvSource({
            "C, 0, 4, C4, 60",
            "C, 1, 4, C#4, 61",
            "D, -1, 4, Db4, 61",
            "B, 1, 3, B#3, 60",
            "C, -1, 4, Cb4, 59",
            "F, 2, 4, F##4, 67",
            "A, 0, 0, A0, 21"
    })
    void soundsAsItsPitch(char step, int alter, int octave, String name, int midi) {
        Spelling spelling = Spelling.of(step, alter, octave);

        assertThat(spelling.toString()).isEqualTo(name);
        assertThat(spelling.pitch()).isEqualTo(Pitch.of(midi));
    }

    @Test
    void spellsAPitchWithSharpsOrFlats() {
        assertThat(Spelling.of(Pitch.of(61), false)).hasToString("C#4");
        assertThat(Spelling.of(Pitch.of(61), true)).hasToString("Db4");
        assertThat(Spelling.of(Pitch.of(60), true)).hasToString("C4");
    }

    @ParameterizedTest
    @ValueSource(chars = {'H', 'c', ' '})
    void rejectsStepsThatAreNotNoteLetters(char step) {
        assertThatIllegalArgumentException().isThrownBy(() -> Spelling.of(step, 0, 4));
    }

    @Test
    void rejectsMoreThanTwoSharpsOrFlats() {
        assertThatIllegalArgumentException().isThrownBy(() -> Spelling.of('C', 3, 4));
    }
}
