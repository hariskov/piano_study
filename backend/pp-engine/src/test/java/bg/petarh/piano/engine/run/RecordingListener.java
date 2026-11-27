package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.Evaluation;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.model.note.Target;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

final class RecordingListener implements SessionListener {

    final List<SessionState> states = new CopyOnWriteArrayList<>();
    final List<Optional<Target>> targets = new CopyOnWriteArrayList<>();
    final List<Evaluation> evaluations = new CopyOnWriteArrayList<>();
    final List<Long> positions = new CopyOnWriteArrayList<>();

    @Override
    public void onStateChanged(SessionState state) {
        states.add(state);
    }

    @Override
    public void onTargetChanged(Optional<Target> target) {
        targets.add(target);
    }

    @Override
    public void onEvaluation(Evaluation evaluation) {
        evaluations.add(evaluation);
    }

    @Override
    public void onPosition(long positionNanos) {
        positions.add(positionNanos);
    }
}
