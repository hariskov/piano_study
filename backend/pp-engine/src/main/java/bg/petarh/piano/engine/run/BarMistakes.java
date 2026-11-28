package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.Evaluation;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.time.Fraction;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.ToIntFunction;

public final class BarMistakes implements SessionListener {

    private final ToIntFunction<Fraction> barAt;
    private final Map<Integer, Integer> counts = new TreeMap<>();
    private Integer currentBar;

    public BarMistakes(ToIntFunction<Fraction> barAt) {
        this.barAt = barAt;
    }

    @Override
    public synchronized void onTargetChanged(Optional<Target> target) {
        target.ifPresent(reached -> currentBar = barAt.applyAsInt(reached.start()));
    }

    @Override
    public synchronized void onEvaluation(Evaluation evaluation) {
        switch (evaluation) {
            case Evaluation.WrongNote ignored when currentBar != null -> counts.merge(currentBar, 1, Integer::sum);
            case Evaluation.MissedNote missed -> counts.merge(barAt.applyAsInt(missed.note().start()), 1, Integer::sum);
            default -> {
            }
        }
    }

    public synchronized Map<Integer, Integer> byBar() {
        return Map.copyOf(counts);
    }
}
