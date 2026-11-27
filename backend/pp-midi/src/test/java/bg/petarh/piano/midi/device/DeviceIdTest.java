package bg.petarh.piano.midi.device;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DeviceIdTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void rejectsBlankIds(String value) {
        assertThatIllegalArgumentException().isThrownBy(() -> DeviceId.of(value));
    }

    @Test
    void idsWithSameValueAreEqual() {
        assertThat(DeviceId.of("hw:1,0")).isEqualTo(DeviceId.of("hw:1,0")).hasSameHashCodeAs(DeviceId.of("hw:1,0"));
        assertThat(DeviceId.of("hw:1,0")).isNotEqualTo(DeviceId.of("hw:2,0"));
    }

    @Test
    void describesItselfByValue() {
        assertThat(DeviceId.of("hw:1,0")).hasToString("hw:1,0");
    }
}
