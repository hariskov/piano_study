package bg.petarh.piano.engine.evaluation;

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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NoScoreEvaluatorTest {

    private static final long MS = 1_000_000L;

    private final ManualClock clock = new ManualClock();
    private final Target c = target("C4", Fraction.ZERO);
    private final Target d = target("D4", Fraction.of(1, 4));

    @Test
    void movesOnByTimeWithoutScoring() {
        Session session = running();
        NoScoreEvaluator evaluator = new NoScoreEvaluator(session);

        assertThat(evaluator.onTick(999 * MS)).isEmpty();
        assertThat(session.currentTarget()).contains(c);

        assertThat(evaluator.onTick(1000 * MS)).isEmpty();
        assertThat(session.currentTarget()).contains(d);
    }

    @Test
    void finishesWhenTheLastNoteHasEnded() {
        Session session = running();
        NoScoreEvaluator evaluator = new NoScoreEvaluator(session);

        evaluator.onTick(1999 * MS);
        assertThat(session.state()).isEqualTo(SessionState.RUNNING);

        evaluator.onTick(2000 * MS);
        assertThat(session.state()).isEqualTo(SessionState.FINISHED);
    }

    @Test
    void ignoresKeyPresses() {
        NoScoreEvaluator evaluator = new NoScoreEvaluator(running());

        assertThat(evaluator.onInput(new InputEvent.NoteOn(Pitch.parse("E4"), Velocity.of(64), 0))).isEmpty();
    }

    @Test
    void listenAndPlayAlongUseIt() {
        Session session = running();

        assertThat(FollowMode.LISTEN.evaluatorFor(session)).isInstanceOf(NoScoreEvaluator.class);
        assertThat(FollowMode.PLAY_ALONG.evaluatorFor(session)).isInstanceOf(NoScoreEvaluator.class);
    }

    private Session running() {
        Session session = new Session(TargetSource.of(List.of(c, d)),
                new SessionTiming(Tempo.of(60), TimeSignature.COMMON_TIME, 0), clock);
        session.start();
        return session;
    }

    private static Target target(String pitch, Fraction start) {
        return Target.of(List.of(new NoteEvent(Pitch.parse(pitch), start, Fraction.of(1, 4), Hand.RIGHT)));
    }
}
