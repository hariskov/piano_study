package bg.petarh.piano.engine.time;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class StopwatchTest {

    private final ManualClock clock = new ManualClock();
    private final Stopwatch stopwatch = new Stopwatch(clock);

    @Test
    void showsNothingBeforeItStarts() {
        clock.advanceNanos(100);

        assertThat(stopwatch.elapsedNanos()).isZero();
        assertThat(stopwatch.isRunning()).isFalse();
    }

    @Test
    void countsTimeWhileRunning() {
        clock.advanceNanos(10);
        stopwatch.start();
        clock.advanceNanos(100);

        assertThat(stopwatch.elapsedNanos()).isEqualTo(100);
        assertThat(stopwatch.isRunning()).isTrue();
    }

    @Test
    void leavesOutPausedTime() {
        stopwatch.start();
        clock.advanceNanos(100);
        stopwatch.pause();
        clock.advanceNanos(50);

        assertThat(stopwatch.elapsedNanos()).isEqualTo(100);

        stopwatch.resume();
        clock.advanceNanos(30);

        assertThat(stopwatch.elapsedNanos()).isEqualTo(130);
    }

    @Test
    void startsOnlyOnce() {
        stopwatch.start();

        assertThatIllegalStateException().isThrownBy(stopwatch::start);
    }

    @Test
    void pausesOnlyWhileRunning() {
        assertThatIllegalStateException().isThrownBy(stopwatch::pause);

        stopwatch.start();
        stopwatch.pause();

        assertThatIllegalStateException().isThrownBy(stopwatch::pause);
    }

    @Test
    void resumesOnlyWhilePaused() {
        assertThatIllegalStateException().isThrownBy(stopwatch::resume);

        stopwatch.start();

        assertThatIllegalStateException().isThrownBy(stopwatch::resume);
    }

    @Test
    void tellsTheElapsedTimeAtAMomentWhileRunning() {
        clock.advanceNanos(1000);
        stopwatch.start();
        clock.advanceNanos(500);

        assertThat(stopwatch.elapsedAt(1200)).isEqualTo(200);
    }

    @Test
    void elapsedTimeAtAMomentStaysPutWhilePaused() {
        stopwatch.start();
        clock.advanceNanos(100);
        stopwatch.pause();
        clock.advanceNanos(100);

        assertThat(stopwatch.elapsedAt(clock.nowNanos())).isEqualTo(100);
    }
}
