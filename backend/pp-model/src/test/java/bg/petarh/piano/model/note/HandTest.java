package bg.petarh.piano.model.note;

import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.pitch.Pitch;
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

    @Test
    void rightHandTakesMiddleCAndAbove() {
        assertThat(Hand.RIGHT.portionOf(range("C2", "C6"))).contains(range("C4", "C6"));
    }

    @Test
    void leftHandTakesTheKeysBelowMiddleC() {
        assertThat(Hand.LEFT.portionOf(range("C2", "C6"))).contains(range("C2", "B3"));
    }

    @Test
    void handHasNoPortionOfARangeOnTheOtherSide() {
        assertThat(Hand.LEFT.portionOf(range("C4", "C6"))).isEmpty();
        assertThat(Hand.RIGHT.portionOf(range("C2", "B3"))).isEmpty();
    }

    private static KeyboardRange range(String lowest, String highest) {
        return KeyboardRange.of(Pitch.parse(lowest), Pitch.parse(highest));
    }
}
