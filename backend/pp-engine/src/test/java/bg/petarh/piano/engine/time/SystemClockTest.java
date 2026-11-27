package bg.petarh.piano.engine.time;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemClockTest {

    @Test
    void neverGoesBack() {
        Clock clock = SystemClock.INSTANCE;

        long first = clock.nowNanos();

        assertThat(clock.nowNanos()).isGreaterThanOrEqualTo(first);
    }
}
