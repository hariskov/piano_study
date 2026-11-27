package bg.petarh.piano.engine.targets;

import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.note.Targets;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

final class MeasureTargets implements TargetSource {

    private static final int MEASURES_AHEAD = 4;

    private final Supplier<List<NoteEvent>> measures;
    private final Deque<Deque<Target>> buffered = new ArrayDeque<>();

    MeasureTargets(Supplier<List<NoteEvent>> measures) {
        this.measures = measures;
    }

    @Override
    public Optional<Target> next() {
        fill();
        return Optional.ofNullable(buffered.peekFirst()).map(Deque::pollFirst);
    }

    @Override
    public List<Target> peek(int count) {
        fill();
        List<Target> upcoming = new ArrayList<>(count);
        for (Deque<Target> measure : buffered) {
            for (Target target : measure) {
                if (upcoming.size() == count) {
                    return upcoming;
                }
                upcoming.add(target);
            }
        }
        while (upcoming.size() < count) {
            Deque<Target> measure = read();
            buffered.addLast(measure);
            measure.stream().limit(count - upcoming.size()).forEach(upcoming::add);
        }
        return upcoming;
    }

    private void fill() {
        while (!buffered.isEmpty() && buffered.peekFirst().isEmpty()) {
            buffered.removeFirst();
        }
        while (buffered.size() <= MEASURES_AHEAD) {
            buffered.addLast(read());
        }
    }

    private Deque<Target> read() {
        return new ArrayDeque<>(Targets.from(measures.get()));
    }
}
