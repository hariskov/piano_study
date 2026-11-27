package bg.petarh.piano.engine.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ManualClockTest {

    @Test
    void startsAtZero() {
        assertThat(new ManualClock().nowNanos()).isZero();
    }

    @Test
    void movesOnlyWhenAdvanced() {
        ManualClock clock = new ManualClock();

        clock.advance(Duration.ofMillis(250));
        clock.advanceNanos(5);

        assertThat(clock.nowNanos()).isEqualTo(250_000_005L);
    }

    @Test
    void neverGoesBack() {
        ManualClock clock = new ManualClock();

        assertThatIllegalArgumentException().isThrownBy(() -> clock.advanceNanos(-1));
        assertThatIllegalArgumentException().isThrownBy(() -> clock.advance(Duration.ofMillis(-1)));
    }
}
