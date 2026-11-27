package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.FollowMode;
import bg.petarh.piano.engine.metronome.ClickOutput;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.engine.time.Clock;
import bg.petarh.piano.engine.time.SystemClock;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static bg.petarh.piano.engine.run.RunFixtures.QUARTER;
import static bg.petarh.piano.engine.run.RunFixtures.press;
import static bg.petarh.piano.engine.run.RunFixtures.target;
import static org.assertj.core.api.Assertions.assertThat;

@Timeout(5)
class PracticeTest {

    private final Clock clock = SystemClock.INSTANCE;
    private final RecordingListener listener = new RecordingListener();
    private final List<Boolean> clicks = new CopyOnWriteArrayList<>();

    @Test
    void runsAWaitSessionFromTheInputToTheEnd() throws Exception {
        try (Practice practice = practice(FollowMode.WAIT, 0, Optional.empty())) {
            var done = practice.start().toCompletableFuture();

            practice.input().onEvent(press("C4", clock.nowNanos()));
            practice.input().onEvent(press("D4", clock.nowNanos()));

            done.get(2, TimeUnit.SECONDS);
            assertThat(practice.elapsedNanos()).isPositive();
        }
        assertThat(listener.states).containsExactly(SessionState.RUNNING, SessionState.FINISHED);
    }

    @Test
    void playsTheMetronomeWhenGivenClicks() throws Exception {
        try (Practice practice = practice(FollowMode.WAIT, 1, Optional.of(clicks::add))) {
            practice.start();
            Thread.sleep(50);
        }
        assertThat(clicks).startsWith(true);
    }

    @Test
    void stoppingFinishesTheSession() throws Exception {
        Practice practice = practice(FollowMode.TEMPO, 1, Optional.empty());
        var done = practice.start().toCompletableFuture();

        practice.stop();

        done.get(2, TimeUnit.SECONDS);
        assertThat(listener.states).endsWith(SessionState.FINISHED);
    }

    @Test
    void pausesResumesAndChangesTempoOnTheSessionThread() throws Exception {
        try (Practice practice = practice(FollowMode.WAIT, 0, Optional.empty())) {
            practice.start();
            practice.pause();
            practice.changeTempo(Tempo.of(60));
            practice.resume();
        }
        assertThat(listener.states).containsExactly(
                SessionState.RUNNING, SessionState.PAUSED, SessionState.RUNNING, SessionState.FINISHED);
    }

    @Test
    void runsChangesOnTheSessionThread() throws Exception {
        CompletableFuture<String> thread = new CompletableFuture<>();
        try (Practice practice = practice(FollowMode.WAIT, 0, Optional.empty())) {
            practice.start();
            practice.onSessionThread(() -> thread.complete(Thread.currentThread().getName()));

            assertThat(thread.get(2, TimeUnit.SECONDS)).isEqualTo("session");
        }
    }

    private Practice practice(FollowMode mode, int countInBars, Optional<ClickOutput> output) {
        return Practice.prepare(
                TargetSource.of(List.of(target("C4", Fraction.ZERO), target("D4", QUARTER))),
                new SessionTiming(Tempo.of(120), TimeSignature.COMMON_TIME, countInBars),
                mode, clock, listener, output);
    }
}
