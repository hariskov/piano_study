package bg.petarh.piano.engine.evaluation;

import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.model.note.Target;

import java.util.List;

public final class NoScoreEvaluator implements Evaluator {

    private final Session session;

    public NoScoreEvaluator(Session session) {
        this.session = session;
    }

    @Override
    public List<Evaluation> onInput(InputEvent event) {
        return List.of();
    }

    @Override
    public List<Evaluation> onTick(long nowNanos) {
        long time = session.runningNanosAt(nowNanos);
        while (session.state() == SessionState.RUNNING) {
            Target current = session.currentTarget().orElseThrow();
            List<Target> upcoming = session.upcoming(1);
            long boundary = upcoming.isEmpty()
                    ? session.nanosAt(current.start().plus(current.duration()))
                    : session.nanosAt(upcoming.getFirst().start());
            if (time < boundary) {
                break;
            }
            session.advance();
        }
        return List.of();
    }
}
