package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.Evaluation;
import bg.petarh.piano.engine.evaluation.Evaluator;
import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.engine.time.Clock;
import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.model.note.Target;

import java.util.List;
import java.util.Optional;

public final class SessionDriver {

    private final Session session;
    private final Evaluator evaluator;
    private final Clock clock;
    private final SessionListener listener;
    private SessionState announcedState;
    private Optional<Target> announcedTarget = Optional.empty();

    public SessionDriver(Session session, Evaluator evaluator, Clock clock, SessionListener listener) {
        this.session = session;
        this.evaluator = evaluator;
        this.clock = clock;
        this.listener = listener;
        this.announcedState = session.state();
    }

    public void start() {
        session.start();
        announceChanges();
        announcePosition();
    }

    public void handle(InputEvent event) {
        report(evaluator.onInput(event));
    }

    public void tick() {
        session.tick();
        report(evaluator.onTick(clock.nowNanos()));
        announcePosition();
    }

    public void pause() {
        if (session.state() == SessionState.RUNNING) {
            session.pause();
            announceChanges();
        }
    }

    public void resume() {
        if (session.state() == SessionState.PAUSED) {
            session.resume();
            announceChanges();
        }
    }

    public void stop() {
        session.stop();
        announceChanges();
    }

    public boolean isFinished() {
        return session.state() == SessionState.FINISHED;
    }

    private void report(List<Evaluation> evaluations) {
        evaluations.forEach(listener::onEvaluation);
        announceChanges();
    }

    private void announcePosition() {
        if (session.state() == SessionState.COUNT_IN || session.state() == SessionState.RUNNING) {
            listener.onPosition(session.positionNanosAt(clock.nowNanos()));
        }
    }

    private void announceChanges() {
        Optional<Target> target = session.currentTarget();
        if (!target.equals(announcedTarget)) {
            announcedTarget = target;
            listener.onTargetChanged(target);
        }
        if (session.state() != announcedState) {
            announcedState = session.state();
            listener.onStateChanged(announcedState);
        }
    }
}
