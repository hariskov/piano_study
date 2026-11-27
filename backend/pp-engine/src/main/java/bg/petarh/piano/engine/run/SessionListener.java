package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.Evaluation;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.model.note.Target;

import java.util.List;
import java.util.Optional;

public interface SessionListener {

    default void onStateChanged(SessionState state) {
    }

    default void onTargetChanged(Optional<Target> target) {
    }

    default void onEvaluation(Evaluation evaluation) {
    }

    default void onPosition(long positionNanos) {
    }

    static SessionListener all(List<SessionListener> listeners) {
        List<SessionListener> copy = List.copyOf(listeners);
        return new SessionListener() {
            @Override
            public void onStateChanged(SessionState state) {
                copy.forEach(listener -> listener.onStateChanged(state));
            }

            @Override
            public void onTargetChanged(Optional<Target> target) {
                copy.forEach(listener -> listener.onTargetChanged(target));
            }

            @Override
            public void onEvaluation(Evaluation evaluation) {
                copy.forEach(listener -> listener.onEvaluation(evaluation));
            }

            @Override
            public void onPosition(long positionNanos) {
                copy.forEach(listener -> listener.onPosition(positionNanos));
            }
        };
    }
}
