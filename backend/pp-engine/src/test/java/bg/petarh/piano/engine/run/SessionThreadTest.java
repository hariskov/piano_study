package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.WaitEvaluator;
import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.engine.time.Clock;
import bg.petarh.piano.engine.time.SystemClock;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static bg.petarh.piano.engine.run.RunFixtures.QUARTER;
import static bg.petarh.piano.engine.run.RunFixtures.press;
import static bg.petarh.piano.engine.run.RunFixtures.target;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(5)
class SessionThreadTest {

    private final Clock clock = SystemClock.INSTANCE;
    private final RecordingListener listener = new RecordingListener();
    private final Target c = target("C4", Fraction.ZERO);
    private final Target d = target("D4", QUARTER);

    @Test
    void runsTheSessionUntilItFinishes() throws Exception {
        try (SessionThread thread = new SessionThread(driver(), Duration.ofMillis(5))) {
            var done = thread.start().toCompletableFuture();

            thread.onEvent(press("C4", clock.nowNanos()));
            thread.onEvent(press("D4", clock.nowNanos()));

            done.get(2, TimeUnit.SECONDS);
        }
        assertThat(listener.states).containsExactly(SessionState.RUNNING, SessionState.FINISHED);
    }

    @Test
    void closingStopsARunningSession() throws Exception {
        SessionThread thread = new SessionThread(driver(), Duration.ofMillis(5));
        var done = thread.start().toCompletableFuture();

        thread.close();

        done.get(2, TimeUnit.SECONDS);
        assertThat(listener.states).endsWith(SessionState.FINISHED);
    }

    @Test
    void pauseAndResumeRunOnTheSessionThread() throws Exception {
        try (SessionThread thread = new SessionThread(driver(), Duration.ofMillis(5))) {
            thread.start();
            thread.pause();
            thread.resume();
        }
        assertThat(listener.states).containsExactly(
                SessionState.RUNNING, SessionState.PAUSED, SessionState.RUNNING, SessionState.FINISHED);
    }

    private SessionDriver driver() {
        Session session = new Session(TargetSource.of(List.of(c, d)),
                new SessionTiming(Tempo.of(120), TimeSignature.COMMON_TIME, 0), clock);
        return new SessionDriver(session, new WaitEvaluator(session), clock, listener);
    }
}
