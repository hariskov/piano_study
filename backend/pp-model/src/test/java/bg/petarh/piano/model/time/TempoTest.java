package bg.petarh.piano.model.time;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TempoTest {

    private static final Fraction QUARTER = Fraction.of(1, 4);
    private static final Fraction EIGHTH = Fraction.of(1, 8);

    @ParameterizedTest
    @ValueSource(ints = {19, 241})
    void rejectsBpmOutsideSupportedRange(int bpm) {
        assertThatIllegalArgumentException().isThrownBy(() -> Tempo.of(bpm));
    }

    @ParameterizedTest
    @ValueSource(ints = {20, 240})
    void acceptsBpmAtTheLimits(int bpm) {
        assertThat(Tempo.of(bpm).bpm()).isEqualTo(bpm);
    }

    @Test
    void defaultsToQuarterNoteBeat() {
        assertThat(Tempo.of(90).beatUnit()).isEqualTo(QUARTER);
    }

    @Test
    void rejectsNonPositiveBeatUnit() {
        assertThatIllegalArgumentException().isThrownBy(() -> Tempo.of(90, Fraction.ZERO));
    }

    @ParameterizedTest
    @CsvSource({
            "60, 1, 4, 1_000_000_000",
            "120, 1, 4, 500_000_000",
            "120, 1, 8, 250_000_000",
            "120, 1, 1, 2_000_000_000",
            "90, 1, 4, 666_666_667"
    })
    void convertsNoteDurationsToNanoseconds(int bpm, long numerator, long denominator, long expectedNanos) {
        assertThat(Tempo.of(bpm).nanosFor(Fraction.of(numerator, denominator))).isEqualTo(expectedNanos);
    }

    @Test
    void honoursTheBeatUnit() {
        Tempo dottedQuarterBeat = Tempo.of(60, Fraction.of(3, 8));

        assertThat(dottedQuarterBeat.nanosFor(Fraction.of(3, 8))).isEqualTo(1_000_000_000L);
        assertThat(dottedQuarterBeat.nanosFor(EIGHTH)).isEqualTo(333_333_333L);
    }

    @Test
    void adjustsBpmWithinTheSupportedRange() {
        assertThat(Tempo.of(90).adjustedBy(5)).isEqualTo(Tempo.of(95));
        assertThat(Tempo.of(238).adjustedBy(5)).isEqualTo(Tempo.of(240));
        assertThat(Tempo.of(22).adjustedBy(-5)).isEqualTo(Tempo.of(20));
    }

    @Test
    void keepsBeatUnitWhenChangingBpm() {
        Tempo tempo = Tempo.of(60, EIGHTH);

        assertThat(tempo.withBpm(100)).isEqualTo(Tempo.of(100, EIGHTH));
    }

    @Test
    void namesItsTempoMarking() {
        assertThat(Tempo.of(90).marking()).isEqualTo(TempoMarking.ANDANTE);
    }

    @Test
    void describesItselfInBpm() {
        assertThat(Tempo.of(90)).hasToString("90 BPM (Andante)");
    }
}
