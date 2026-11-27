package bg.petarh.piano.engine.targets;

import bg.petarh.piano.model.note.Target;

import java.util.List;
import java.util.Optional;

final class ListTargets implements TargetSource {

    private final List<Target> targets;
    private int position;

    ListTargets(List<Target> targets) {
        this.targets = List.copyOf(targets);
    }

    @Override
    public Optional<Target> next() {
        return position < targets.size() ? Optional.of(targets.get(position++)) : Optional.empty();
    }

    @Override
    public List<Target> peek(int count) {
        return targets.subList(position, Math.min(targets.size(), position + count));
    }
}
