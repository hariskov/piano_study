package bg.petarh.piano.model.time;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class FractionTest {

    @ParameterizedTest
    @CsvSource({
            "2, 4, 1, 2",
            "6, 8, 3, 4",
            "-2, -4, 1, 2",
            "2, -4, -1, 2",
            "0, 5, 0, 1"
    })
    void reducesToLowestTermsWithPositiveDenominator(long numerator, long denominator, long expectedNumerator,
                                                     long expectedDenominator) {
        Fraction fraction = Fraction.of(numerator, denominator);

        assertThat(fraction.numerator()).isEqualTo(expectedNumerator);
        assertThat(fraction.denominator()).isEqualTo(expectedDenominator);
    }

    @Test
    void rejectsZeroDenominator() {
        assertThatIllegalArgumentException().isThrownBy(() -> Fraction.of(1, 0));
    }

    @Test
    void addsAndSubtracts() {
        assertThat(Fraction.of(1, 4).plus(Fraction.of(1, 8))).isEqualTo(Fraction.of(3, 8));
        assertThat(Fraction.of(1, 4).minus(Fraction.of(3, 8))).isEqualTo(Fraction.of(-1, 8));
    }

    @Test
    void multipliesAndDivides() {
        assertThat(Fraction.of(3, 8).times(Fraction.of(2, 3))).isEqualTo(Fraction.of(1, 4));
        assertThat(Fraction.of(1, 4).times(3)).isEqualTo(Fraction.of(3, 4));
        assertThat(Fraction.of(3, 4).dividedBy(Fraction.of(1, 4))).isEqualTo(Fraction.whole(3));
    }

    @Test
    void rejectsDivisionByZero() {
        assertThatExceptionOfType(ArithmeticException.class)
                .isThrownBy(() -> Fraction.of(1, 4).dividedBy(Fraction.ZERO));
    }

    @Test
    void detectsOverflowInsteadOfWrappingAround() {
        Fraction huge = Fraction.whole(Long.MAX_VALUE);

        assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> huge.plus(Fraction.whole(1)));
    }

    @Test
    void comparesByValue() {
        assertThat(Fraction.of(1, 3)).isLessThan(Fraction.of(1, 2));
        assertThat(Fraction.of(2, 4)).isEqualByComparingTo(Fraction.of(1, 2));
        assertThat(Fraction.of(-1, 2)).isLessThan(Fraction.ZERO);
    }

    @Test
    void equalFractionsHaveEqualHashCodes() {
        assertThat(Fraction.of(2, 4)).isEqualTo(Fraction.of(1, 2)).hasSameHashCodeAs(Fraction.of(1, 2));
    }

    @Test
    void reportsSign() {
        assertThat(Fraction.ZERO.isZero()).isTrue();
        assertThat(Fraction.of(1, 8).isPositive()).isTrue();
        assertThat(Fraction.of(-1, 8).isPositive()).isFalse();
    }

    @Test
    void describesItselfAsNumeratorOverDenominator() {
        assertThat(Fraction.of(6, 8)).hasToString("3/4");
        assertThat(Fraction.whole(2)).hasToString("2");
    }

    @ParameterizedTest
    @CsvSource({
            "7, 2, 3, 4",
            "6, 2, 3, 3",
            "-7, 2, -4, -3",
            "0, 1, 0, 0"
    })
    void roundsDownAndUpToWholeNumbers(long numerator, long denominator, long floor, long ceil) {
        Fraction fraction = Fraction.of(numerator, denominator);

        assertThat(fraction.floor()).isEqualTo(floor);
        assertThat(fraction.ceil()).isEqualTo(ceil);
    }
}
