package bg.petarh.piano.engine.evaluation;

import bg.petarh.piano.engine.evaluation.Evaluation.MissedNote;
import bg.petarh.piano.engine.evaluation.Evaluation.TimedNote;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TempoEvaluatorTest {

    private static final Fraction QUARTER = Fraction.of(1, 4);
    private static final long MS = 1_000_000L;

    private final ManualClock clock = new ManualClock();
    private final NoteEvent c = note("C4", Fraction.ZERO);
    private final NoteEvent d = note("D4", QUARTER);
    private final NoteEvent e = note("E4", Fraction.of(1, 2));
    private final NoteEvent g = note("G4", Fraction.of(1, 2));

    @Test
    void pressOnTheBeatIsPerfect() {
        TempoEvaluator evaluator = new TempoEvaluator(running(120, 0, targets()));

        assertThat(evaluator.onInput(press("C4", 0))).containsExactly(new TimedNote(c, TimingGrade.PERFECT, 0, 0));
    }

    @Test
    void gradesEarlyAndLatePresses() {
        TempoEvaluator evaluator = new TempoEvaluator(running(120, 0, targets()));

        assertThat(evaluator.onInput(press("C4", 70)))
                .containsExactly(new TimedNote(c, TimingGrade.GOOD, 70 * MS, 70 * MS));
        assertThat(evaluator.onInput(press("D4", 380)))
                .containsExactly(new TimedNote(d, TimingGrade.OK, -120 * MS, 380 * MS));
    }

    @Test
    void pressOutsideEveryWindowIsWrong() {
        TempoEvaluator evaluator = new TempoEvaluator(running(120, 0, targets()));

        assertThat(evaluator.onInput(press("D4", 340))).containsExactly(new WrongNote(pitch("D4"), 340 * MS));
        assertThat(evaluator.onInput(press("A4", 0))).containsExactly(new WrongNote(pitch("A4"), 0));
        assertThat(evaluator.wrongKeys()).isEqualTo(2);
    }

    @Test
    void eachNoteIsMatchedOnlyOnce() {
        TempoEvaluator evaluator = new TempoEvaluator(running(120, 0, targets()));

        evaluator.onInput(press("C4", 0));

        assertThat(evaluator.onInput(press("C4", 20))).containsExactly(new WrongNote(pitch("C4"), 20 * MS));
    }

    @Test
    void pressMatchesTheNearestNoteOfThatPitch() {
        NoteEvent first = note("C4", Fraction.ZERO);
        NoteEvent second = note("C4", QUARTER);
        TempoEvaluator evaluator = new TempoEvaluator(running(240, 0, List.of(target(first), target(second))));

        assertThat(evaluator.onInput(press("C4", 130)))
                .containsExactly(new TimedNote(second, TimingGrade.OK, -120 * MS, 130 * MS));
    }

    @Test
    void chordNotesAreGradedOneByOne() {
        TempoEvaluator evaluator = new TempoEvaluator(running(120, 0, targets()));

        assertThat(evaluator.onInput(press("E4", 1000))).extracting(r -> ((TimedNote) r).grade())
                .containsExactly(TimingGrade.PERFECT);
        assertThat(evaluator.onInput(press("G4", 1060))).extracting(r -> ((TimedNote) r).grade())
                .containsExactly(TimingGrade.GOOD);
    }

    @Test
    void noteIsMissedOnceItsWindowHasPassed() {
        Session session = running(120, 0, targets());
        TempoEvaluator evaluator = new TempoEvaluator(session);

        clock.advance(Duration.ofMillis(150));
        assertThat(evaluator.onTick(clock.nowNanos())).isEmpty();

        clock.advance(Duration.ofMillis(1));
        assertThat(evaluator.onTick(clock.nowNanos())).containsExactly(new MissedNote(c, 151 * MS));
        assertThat(session.currentTarget()).get().extracting(Target::notes).isEqualTo(List.of(d));
        assertThat(evaluator.misses()).isEqualTo(1);
    }

    @Test
    void sessionFinishesAfterTheLastWindowCloses() {
        Session session = running(120, 0, targets());
        TempoEvaluator evaluator = new TempoEvaluator(session);
        evaluator.onInput(press("C4", 0));
        evaluator.onInput(press("D4", 500));
        evaluator.onInput(press("E4", 1000));

        clock.advance(Duration.ofMillis(1151));
        List<Evaluation> results = evaluator.onTick(clock.nowNanos());

        assertThat(results).containsExactly(new MissedNote(g, 1151 * MS));
        assertThat(session.state()).isEqualTo(SessionState.FINISHED);
    }

    @Test
    void countInAndPausesShiftWhenNotesAreExpected() {
        Session session = running(120, 1, targets());
        TempoEvaluator evaluator = new TempoEvaluator(session);
        clock.advance(Duration.ofSeconds(2));
        session.tick();

        clock.advance(Duration.ofMillis(100));
        session.pause();
        clock.advance(Duration.ofSeconds(5));
        session.resume();
        clock.advance(Duration.ofMillis(400));

        assertThat(evaluator.onInput(press("D4", clock.nowNanos() / MS)))
                .extracting(r -> ((TimedNote) r).grade()).containsExactly(TimingGrade.PERFECT);
    }

    @Test
    void ignoresKeysWhenNotRunning() {
        Session session = running(120, 1, targets());
        TempoEvaluator evaluator = new TempoEvaluator(session);

        assertThat(evaluator.onInput(press("C4", 0))).isEmpty();
        assertThat(evaluator.onTick(0)).isEmpty();
    }

    @Test
    void countsEachGrade() {
        TempoEvaluator evaluator = new TempoEvaluator(running(120, 0, targets()));

        evaluator.onInput(press("C4", 0));
        evaluator.onInput(press("D4", 600));
        evaluator.onInput(press("E4", 1010));

        assertThat(evaluator.gradeCount(TimingGrade.PERFECT)).isEqualTo(2);
        assertThat(evaluator.gradeCount(TimingGrade.GOOD)).isEqualTo(1);
        assertThat(evaluator.gradeCount(TimingGrade.OK)).isZero();
    }

    private Session running(int bpm, int countInBars, List<Target> targets) {
        Session session = new Session(TargetSource.of(targets),
                new SessionTiming(Tempo.of(bpm), TimeSignature.COMMON_TIME, countInBars), clock);
        session.start();
        return session;
    }

    private List<Target> targets() {
        List<Target> targets = new ArrayList<>();
        targets.add(target(c));
        targets.add(target(d));
        targets.add(Target.of(List.of(e, g)));
        return targets;
    }

    private static Target target(NoteEvent note) {
        return Target.of(List.of(note));
    }

    private static InputEvent press(String pitch, long atMillis) {
        return new InputEvent.NoteOn(pitch(pitch), Velocity.of(64), atMillis * MS);
    }

    private static Pitch pitch(String name) {
        return Pitch.parse(name);
    }

    private static NoteEvent note(String pitch, Fraction start) {
        return new NoteEvent(pitch(pitch), start, QUARTER, Hand.RIGHT);
    }
}
