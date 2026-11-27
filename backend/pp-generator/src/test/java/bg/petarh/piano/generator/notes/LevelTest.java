package bg.petarh.piano.generator.notes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LevelTest {

    @Test
    void findsLevelByNumber() {
        assertThat(Level.of(1)).isEqualTo(Level.ONE);
        assertThat(Level.of(2)).isEqualTo(Level.TWO);
        assertThat(Level.of(3)).isEqualTo(Level.THREE);
        assertThat(Level.of(4)).isEqualTo(Level.FOUR);
        assertThat(Level.TWO.number()).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void rejectsUnknownLevel(int number) {
        assertThatIllegalArgumentException().isThrownBy(() -> Level.of(number));
    }
}
