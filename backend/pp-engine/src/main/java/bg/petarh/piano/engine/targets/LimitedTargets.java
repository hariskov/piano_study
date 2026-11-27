package bg.petarh.piano.engine.targets;

import bg.petarh.piano.model.note.Target;

import java.util.List;
import java.util.Optional;

final class LimitedTargets implements TargetSource {

    private final TargetSource source;
    private int remaining;

    LimitedTargets(TargetSource source, int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("A session needs at least one target: " + limit);
        }
        this.source = source;
        this.remaining = limit;
    }

    @Override
    public Optional<Target> next() {
        if (remaining == 0) {
            return Optional.empty();
        }
        Optional<Target> target = source.next();
        target.ifPresent(present -> remaining--);
        return target;
    }

    @Override
    public void discardLookAhead() {
        source.discardLookAhead();
    }

    @Override
    public List<Target> peek(int count) {
        return source.peek(Math.min(count, remaining));
    }
}
