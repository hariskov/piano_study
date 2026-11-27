package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.Evaluation;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.model.note.Target;

import java.util.Optional;

public interface SessionListener {

    default void onStateChanged(SessionState state) {
    }

    default void onTargetChanged(Optional<Target> target) {
    }

    default void onEvaluation(Evaluation evaluation) {
    }
}
