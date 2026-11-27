package bg.petarh.piano.engine.session;

import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.engine.time.ManualClock;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class SessionTest {

    private static final Fraction QUARTER = Fraction.of(1, 4);

    private final ManualClock clock = new ManualClock();
    private final Target first = target("C4", Fraction.ZERO);
    private final Target second = target("D4", QUARTER);
    private final Target third = target("E4", Fraction.of(1, 2));

    @Test
    void waitsToBeStarted() {
        Session session = session(0);

        assertThat(session.state()).isEqualTo(SessionState.READY);
        assertThat(session.currentTarget()).isEmpty();
    }

    @Test
    void startsRunningStraightAwayWithoutCountIn() {
        Session session = session(0);

        session.start();

        assertThat(session.state()).isEqualTo(SessionState.RUNNING);
        assertThat(session.currentTarget()).contains(first);
    }

    @Test
    void countsInBeforeRunning() {
        Session session = session(1);
        session.start();

        clock.advance(Duration.ofMillis(1999));
        session.tick();
        assertThat(session.state()).isEqualTo(SessionState.COUNT_IN);
        assertThat(session.currentTarget()).contains(first);

        clock.advance(Duration.ofMillis(1));
        session.tick();
        assertThat(session.state()).isEqualTo(SessionState.RUNNING);
    }

    @Test
    void countsElapsedTimeWithoutCountInAndPauses() {
        Session session = session(1);
        session.start();
        clock.advance(Duration.ofSeconds(2));
        session.tick();

        clock.advance(Duration.ofSeconds(3));
        session.pause();
        clock.advance(Duration.ofSeconds(10));
        session.resume();
        clock.advance(Duration.ofSeconds(1));

        assertThat(session.elapsedNanos()).isEqualTo(Duration.ofSeconds(4).toNanos());
    }

    @Test
    void convertsAKeyPressTimeToRunningTime() {
        Session session = session(1);
        session.start();
        clock.advance(Duration.ofSeconds(2));
        session.tick();
        clock.advance(Duration.ofSeconds(1));

        assertThat(session.runningNanosAt(Duration.ofMillis(2500).toNanos()))
                .isEqualTo(Duration.ofMillis(500).toNanos());
    }

    @Test
    void positionIsNegativeDuringTheCountInAndZeroAtTheFirstDownbeat() {
        Session session = session(1);
        session.start();

        clock.advance(Duration.ofMillis(500));
        assertThat(session.positionNanosAt(clock.nowNanos())).isEqualTo(-Duration.ofMillis(1500).toNanos());

        clock.advance(Duration.ofMillis(1500));
        session.tick();
        assertThat(session.positionNanosAt(clock.nowNanos())).isZero();
    }

    @Test
    void positionStandsStillWhilePaused() {
        Session session = session(0);
        session.start();
        clock.advance(Duration.ofMillis(300));
        session.pause();
        clock.advance(Duration.ofSeconds(4));

        assertThat(session.positionNanosAt(clock.nowNanos())).isEqualTo(Duration.ofMillis(300).toNanos());
    }

    @Test
    void movesThroughTheTargetsAndFinishesAfterTheLast() {
        Session session = session(0);
        session.start();

        session.advance();
        assertThat(session.currentTarget()).contains(second);
        session.advance();
        assertThat(session.currentTarget()).contains(third);
        session.advance();

        assertThat(session.state()).isEqualTo(SessionState.FINISHED);
        assertThat(session.currentTarget()).isEmpty();
    }

    @Test
    void showsWhatComesAfterTheCurrentTarget() {
        Session session = session(0);
        session.start();

        assertThat(session.upcoming(5)).containsExactly(second, third);
    }

    @Test
    void finishesAtOnceWithNothingToPlay() {
        Session session = new Session(TargetSource.of(List.of()), timing(0), clock);

        session.start();

        assertThat(session.state()).isEqualTo(SessionState.FINISHED);
    }

    @Test
    void pausesAndResumes() {
        Session session = session(0);
        session.start();

        session.pause();
        assertThat(session.state()).isEqualTo(SessionState.PAUSED);
        session.resume();
        assertThat(session.state()).isEqualTo(SessionState.RUNNING);
    }

    @Test
    void canBeStoppedAtAnyTime() {
        Session ready = session(0);
        Session countingIn = session(1);
        countingIn.start();
        Session paused = session(0);
        paused.start();
        paused.pause();

        for (Session session : List.of(ready, countingIn, paused)) {
            session.stop();
            session.stop();
            assertThat(session.state()).isEqualTo(SessionState.FINISHED);
        }
    }

    @Test
    void rejectsActionsThatDoNotFitTheState() {
        Session session = session(1);

        assertThatIllegalStateException().isThrownBy(session::pause);
        assertThatIllegalStateException().isThrownBy(session::advance);
        session.start();
        assertThatIllegalStateException().isThrownBy(session::start);
        assertThatIllegalStateException().isThrownBy(session::advance);
        assertThatIllegalStateException().isThrownBy(session::resume);
        clock.advance(Duration.ofSeconds(2));
        session.tick();
        session.pause();
        assertThatIllegalStateException().isThrownBy(session::advance);
    }

    private Session session(int countInBars) {
        return new Session(TargetSource.of(List.of(first, second, third)), timing(countInBars), clock);
    }

    private static SessionTiming timing(int countInBars) {
        return new SessionTiming(Tempo.of(120), TimeSignature.COMMON_TIME, countInBars);
    }

    private static Target target(String pitch, Fraction start) {
        return Target.of(List.of(new NoteEvent(Pitch.parse(pitch), start, QUARTER, Hand.RIGHT)));
    }
}
