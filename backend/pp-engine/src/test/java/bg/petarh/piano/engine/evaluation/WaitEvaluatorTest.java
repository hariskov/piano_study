package bg.petarh.piano.engine.evaluation;

import bg.petarh.piano.engine.evaluation.Evaluation.CorrectNote;
import bg.petarh.piano.engine.evaluation.Evaluation.TargetCompleted;
import bg.petarh.piano.engine.evaluation.Evaluation.WrongNote;
import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.engine.time.ManualClock;
import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class WaitEvaluatorTest {

    private static final Fraction QUARTER = Fraction.of(1, 4);
    private static final long MS = 1_000_000L;

    private final ManualClock clock = new ManualClock();
    private final Target cMajor = target(Fraction.ZERO, "C4", "E4", "G4");
    private final Target d = target(QUARTER, "D4");

    @Test
    void correctNoteCompletesASingleNoteTarget() {
        Session session = running(d);
        WaitEvaluator evaluator = new WaitEvaluator(session);

        List<Evaluation> results = evaluator.onInput(press("D4", 10));

        assertThat(results).containsExactly(new CorrectNote(pitch("D4"), 10 * MS), new TargetCompleted(d, 10 * MS));
        assertThat(session.state()).isEqualTo(SessionState.FINISHED);
        assertThat(evaluator.completedTargets()).isEqualTo(1);
    }

    @Test
    void wrongKeyIsCountedAndTheMusicWaits() {
        Session session = running(d, cMajor);
        WaitEvaluator evaluator = new WaitEvaluator(session);

        assertThat(evaluator.onInput(press("E4", 5))).containsExactly(new WrongNote(pitch("E4"), 5 * MS));

        assertThat(session.currentTarget()).contains(d);
        assertThat(evaluator.wrongKeys()).isEqualTo(1);
    }

    @Test
    void chordIsCompleteWhenAllItsKeysAreHeldTogether() {
        Session session = running(cMajor, d);
        WaitEvaluator evaluator = new WaitEvaluator(session);

        evaluator.onInput(press("C4", 0));
        evaluator.onInput(press("E4", 1000));
        List<Evaluation> results = evaluator.onInput(press("G4", 2000));

        assertThat(results).contains(new TargetCompleted(cMajor, 2000 * MS));
        assertThat(session.currentTarget()).contains(d);
    }

    @Test
    void chordIsCompleteWhenItsKeysArePressedWithinTheChordWindow() {
        Session session = running(cMajor, d);
        WaitEvaluator evaluator = new WaitEvaluator(session);

        evaluator.onInput(press("C4", 0));
        evaluator.onInput(release("C4", 50));
        evaluator.onInput(press("E4", 100));
        evaluator.onInput(release("E4", 150));
        List<Evaluation> results = evaluator.onInput(press("G4", 300));

        assertThat(results).contains(new TargetCompleted(cMajor, 300 * MS));
    }

    @Test
    void chordSpreadBeyondTheWindowWithoutHoldingIsNotComplete() {
        Session session = running(cMajor, d);
        WaitEvaluator evaluator = new WaitEvaluator(session);

        evaluator.onInput(press("C4", 0));
        evaluator.onInput(release("C4", 50));
        evaluator.onInput(press("E4", 200));
        evaluator.onInput(press("G4", 400));

        assertThat(session.currentTarget()).contains(cMajor);

        assertThat(evaluator.onInput(press("C4", 500))).containsExactly(new TargetCompleted(cMajor, 500 * MS));
    }

    @Test
    void pressingACountedKeyAgainIsIgnored() {
        Session session = running(cMajor, d);
        WaitEvaluator evaluator = new WaitEvaluator(session);

        evaluator.onInput(press("C4", 0));
        evaluator.onInput(release("C4", 50));

        assertThat(evaluator.onInput(press("C4", 100))).isEmpty();
        assertThat(evaluator.wrongKeys()).isZero();
    }

    @Test
    void keyStillHeldFromTheLastTargetMustBePressedAgain() {
        Target dAgain = target(Fraction.of(1, 2), "D4");
        Session session = running(d, dAgain);
        WaitEvaluator evaluator = new WaitEvaluator(session);

        evaluator.onInput(press("D4", 0));

        assertThat(session.currentTarget()).contains(dAgain);
        evaluator.onInput(release("D4", 100));
        assertThat(evaluator.onInput(press("D4", 200))).contains(new TargetCompleted(dAgain, 200 * MS));
    }

    @Test
    void ignoresKeysDuringCountInAndPause() {
        Session session = new Session(TargetSource.of(List.of(d)), timing(1), clock);
        WaitEvaluator evaluator = new WaitEvaluator(session);
        session.start();

        assertThat(evaluator.onInput(press("E4", 0))).isEmpty();

        clock.advance(Duration.ofSeconds(2));
        session.tick();
        session.pause();
        assertThat(evaluator.onInput(press("E4", 2100))).isEmpty();
        assertThat(evaluator.wrongKeys()).isZero();
    }

    @Test
    void ignoresTheSustainPedal() {
        WaitEvaluator evaluator = new WaitEvaluator(running(d));

        assertThat(evaluator.onInput(new InputEvent.SustainPedal(true, 0))).isEmpty();
    }

    @Test
    void chordWindowCanBeChanged() {
        Session session = running(cMajor, d);
        WaitEvaluator evaluator = new WaitEvaluator(session, Duration.ofMillis(100));

        evaluator.onInput(press("C4", 0));
        evaluator.onInput(release("C4", 10));
        evaluator.onInput(press("E4", 50));
        evaluator.onInput(release("E4", 60));
        evaluator.onInput(press("G4", 150));

        assertThat(session.currentTarget()).contains(cMajor);
    }

    @Test
    void rejectsNegativeChordWindow() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new WaitEvaluator(running(d), Duration.ofMillis(-1)));
    }

    private Session running(Target... targets) {
        Session session = new Session(TargetSource.of(List.of(targets)), timing(0), clock);
        session.start();
        return session;
    }

    private static SessionTiming timing(int countInBars) {
        return new SessionTiming(Tempo.of(120), TimeSignature.COMMON_TIME, countInBars);
    }

    private static InputEvent press(String pitch, long atMillis) {
        return new InputEvent.NoteOn(pitch(pitch), Velocity.of(64), atMillis * MS);
    }

    private static InputEvent release(String pitch, long atMillis) {
        return new InputEvent.NoteOff(pitch(pitch), atMillis * MS);
    }

    private static Pitch pitch(String name) {
        return Pitch.parse(name);
    }

    private static Target target(Fraction start, String... pitches) {
        return Target.of(Arrays.stream(pitches)
                .map(name -> new NoteEvent(pitch(name), start, QUARTER, Hand.RIGHT))
                .toList());
    }
}
