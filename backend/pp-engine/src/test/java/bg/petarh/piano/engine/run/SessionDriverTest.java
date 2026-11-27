package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.Evaluation.CorrectNote;
import bg.petarh.piano.engine.evaluation.Evaluation.MissedNote;
import bg.petarh.piano.engine.evaluation.Evaluation.TargetCompleted;
import bg.petarh.piano.engine.evaluation.TempoEvaluator;
import bg.petarh.piano.engine.evaluation.WaitEvaluator;
import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.engine.time.ManualClock;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static bg.petarh.piano.engine.run.RunFixtures.QUARTER;
import static bg.petarh.piano.engine.run.RunFixtures.press;
import static bg.petarh.piano.engine.run.RunFixtures.target;
import static org.assertj.core.api.Assertions.assertThat;

class SessionDriverTest {

    private final ManualClock clock = new ManualClock();
    private final RecordingListener listener = new RecordingListener();
    private final Target c = target("C4", Fraction.ZERO);
    private final Target d = target("D4", QUARTER);

    @Test
    void announcesTheStateAndFirstTargetOnStart() {
        SessionDriver driver = waitDriver(1);

        driver.start();

        assertThat(listener.states).containsExactly(SessionState.COUNT_IN);
        assertThat(listener.targets).containsExactly(Optional.of(c));
    }

    @Test
    void ticksEndTheCountIn() {
        SessionDriver driver = waitDriver(1);
        driver.start();

        clock.advance(Duration.ofSeconds(2));
        driver.tick();

        assertThat(listener.states).containsExactly(SessionState.COUNT_IN, SessionState.RUNNING);
    }

    @Test
    void passesKeyPressesToTheEvaluatorAndAnnouncesWhatChanged() {
        SessionDriver driver = waitDriver(0);
        driver.start();

        driver.handle(press("C4", 0));

        assertThat(listener.evaluations).containsExactly(
                new CorrectNote(Pitch.parse("C4"), 0), new TargetCompleted(c, 0));
        assertThat(listener.targets).containsExactly(Optional.of(c), Optional.of(d));
    }

    @Test
    void announcesTheEndOfTheSession() {
        SessionDriver driver = waitDriver(0);
        driver.start();

        driver.handle(press("C4", 0));
        driver.handle(press("D4", 10));

        assertThat(listener.states).containsExactly(SessionState.RUNNING, SessionState.FINISHED);
        assertThat(listener.targets.getLast()).isEmpty();
        assertThat(driver.isFinished()).isTrue();
    }

    @Test
    void ticksLetTheTempoEvaluatorReportMisses() {
        Session session = session(0);
        SessionDriver driver = new SessionDriver(session, new TempoEvaluator(session), clock, listener);
        driver.start();

        clock.advance(Duration.ofMillis(151));
        driver.tick();

        assertThat(listener.evaluations).containsExactly(new MissedNote(c.notes().getFirst(), 151_000_000L));
        assertThat(listener.targets).containsExactly(Optional.of(c), Optional.of(d));
    }

    @Test
    void pausesResumesAndStops() {
        SessionDriver driver = waitDriver(0);
        driver.start();

        driver.pause();
        driver.resume();
        driver.stop();

        assertThat(listener.states).containsExactly(
                SessionState.RUNNING, SessionState.PAUSED, SessionState.RUNNING, SessionState.FINISHED);
    }

    @Test
    void doesNotAnnounceWhenNothingChanged() {
        SessionDriver driver = waitDriver(0);
        driver.start();

        driver.tick();
        driver.handle(press("E4", 0));

        assertThat(listener.states).containsExactly(SessionState.RUNNING);
        assertThat(listener.targets).containsExactly(Optional.of(c));
    }

    private SessionDriver waitDriver(int countInBars) {
        Session session = session(countInBars);
        return new SessionDriver(session, new WaitEvaluator(session), clock, listener);
    }

    private Session session(int countInBars) {
        return new Session(TargetSource.of(List.of(c, d)),
                new SessionTiming(Tempo.of(120), TimeSignature.COMMON_TIME, countInBars), clock);
    }
}
