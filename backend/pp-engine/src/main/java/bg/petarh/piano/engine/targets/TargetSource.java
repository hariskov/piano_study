package bg.petarh.piano.engine.targets;

import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public interface TargetSource {

    Optional<Target> next();

    List<Target> peek(int count);

    default void discardLookAhead() {
    }

    default TargetSource limitedTo(int count) {
        return new LimitedTargets(this, count);
    }

    static TargetSource of(List<Target> targets) {
        return new ListTargets(targets);
    }

    static TargetSource fromMeasures(Supplier<List<NoteEvent>> measures) {
        return new MeasureTargets(measures);
    }
}
