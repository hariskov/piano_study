package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.Evaluation;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.time.Fraction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

public final class PassResults implements SessionListener {

    private final ToIntFunction<Fraction> passAt;
    private final List<Results> passes = new ArrayList<>();
    private int currentPass = 1;

    public PassResults(ToIntFunction<Fraction> passAt) {
        this.passAt = passAt;
    }

    @Override
    public synchronized void onTargetChanged(Optional<Target> target) {
        target.ifPresent(reached -> currentPass = passAt.applyAsInt(reached.start()));
    }

    @Override
    public synchronized void onEvaluation(Evaluation evaluation) {
        int pass = switch (evaluation) {
            case Evaluation.TimedNote timed -> passAt.applyAsInt(timed.note().start());
            case Evaluation.MissedNote missed -> passAt.applyAsInt(missed.note().start());
            case Evaluation.TargetCompleted completed -> passAt.applyAsInt(completed.target().start());
            case Evaluation.CorrectNote ignored -> currentPass;
            case Evaluation.WrongNote ignored -> currentPass;
        };
        while (passes.size() < pass) {
            passes.add(new Results());
        }
        passes.get(pass - 1).onEvaluation(evaluation);
    }

    public synchronized List<Summary> passes() {
        return passes.stream().map(Results::summary).toList();
    }
}
