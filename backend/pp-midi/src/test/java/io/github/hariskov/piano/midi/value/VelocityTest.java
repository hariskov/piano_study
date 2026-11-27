package io.github.hariskov.piano.midi.value;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class VelocityTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 128})
    void rejectsValuesOutsideAudibleRange(int value) {
        assertThatIllegalArgumentException().isThrownBy(() -> Velocity.of(value));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 64, 127})
    void keepsAudibleValues(int value) {
        assertThat(Velocity.of(value).value()).isEqualTo(value);
    }

    @Test
    void velocitiesWithSameValueAreEqual() {
        assertThat(Velocity.of(80)).isEqualTo(Velocity.of(80)).hasSameHashCodeAs(Velocity.of(80));
        assertThat(Velocity.of(80)).isNotEqualTo(Velocity.of(81));
    }
}
