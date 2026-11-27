package bg.petarh.piano.engine.evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TimingWindowsTest {

    @ParameterizedTest
    @CsvSource({
            "0, PERFECT",
            "50, PERFECT",
            "-50, PERFECT",
            "51, GOOD",
            "-100, GOOD",
            "101, OK",
            "-150, OK"
    })
    void gradesByHowFarOffThePressIs(long offsetMillis, TimingGrade grade) {
        assertThat(TimingWindows.DEFAULT.grade(Duration.ofMillis(offsetMillis).toNanos())).contains(grade);
    }

    @Test
    void pressFurtherOffThanTheWidestWindowHasNoGrade() {
        assertThat(TimingWindows.DEFAULT.grade(Duration.ofMillis(151).toNanos())).isEqualTo(Optional.empty());
    }

    @Test
    void windowsMustWidenFromPerfectToOk() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TimingWindows(
                Duration.ofMillis(100), Duration.ofMillis(50), Duration.ofMillis(150)));
        assertThatIllegalArgumentException().isThrownBy(() -> new TimingWindows(
                Duration.ofMillis(-1), Duration.ofMillis(50), Duration.ofMillis(150)));
    }
}
