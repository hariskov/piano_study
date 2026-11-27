package bg.petarh.piano.model.note;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class HandTest {

    @Test
    void rightHandPlaysTheUpperStaff() {
        assertThat(Hand.RIGHT.staff()).isEqualTo(1);
        assertThat(Hand.forStaff(1)).isEqualTo(Hand.RIGHT);
    }

    @Test
    void leftHandPlaysTheLowerStaff() {
        assertThat(Hand.LEFT.staff()).isEqualTo(2);
        assertThat(Hand.forStaff(2)).isEqualTo(Hand.LEFT);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3})
    void rejectsStaffWithoutAHand(int staff) {
        assertThatIllegalArgumentException().isThrownBy(() -> Hand.forStaff(staff));
    }
}
