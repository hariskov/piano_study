package bg.petarh.piano.engine.evaluation;

import bg.petarh.piano.engine.session.Session;

public enum FollowMode {
    WAIT,
    TEMPO;

    public Evaluator evaluatorFor(Session session) {
        return switch (this) {
            case WAIT -> new WaitEvaluator(session);
            case TEMPO -> new TempoEvaluator(session);
        };
    }
}
