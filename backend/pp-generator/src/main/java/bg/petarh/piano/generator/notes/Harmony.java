package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.pitch.Pitch;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

final class Harmony {

    private static final int CHANCES = 4;
    private static final int SMALLEST_INTERVAL_STEPS = 2;
    private static final int LARGEST_INTERVAL_STEPS = 5;
    private static final List<Integer> TRIAD_STEPS = List.of(2, 4);

    private final List<Pitch> scale;
    private final int direction;
    private final Level level;

    private Harmony(List<Pitch> scale, int direction, Level level) {
        this.scale = scale;
        this.direction = direction;
        this.level = level;
    }

    static Harmony of(List<Pitch> scale, Hand hand, Level level) {
        return new Harmony(scale, hand == Hand.RIGHT ? -1 : 1, level);
    }

    List<Pitch> around(Pitch melody, RandomGenerator random) {
        int position = scale.indexOf(melody);
        int notesTogether = random.nextInt(CHANCES) + 2;
        if (position < 0 || notesTogether > level.mostNotesTogether()) {
            return List.of(melody);
        }
        return notesTogether == 2 ? interval(position, random) : triad(position);
    }

    private List<Pitch> interval(int position, RandomGenerator random) {
        int[] fitting = IntStream.rangeClosed(SMALLEST_INTERVAL_STEPS, LARGEST_INTERVAL_STEPS)
                .filter(steps -> fits(position, steps))
                .toArray();
        if (fitting.length == 0) {
            return List.of(scale.get(position));
        }
        return withSteps(position, List.of(fitting[random.nextInt(fitting.length)]));
    }

    private List<Pitch> triad(int position) {
        return fits(position, TRIAD_STEPS.getLast()) ? withSteps(position, TRIAD_STEPS) : List.of(scale.get(position));
    }

    private boolean fits(int position, int steps) {
        int other = position + direction * steps;
        return other >= 0 && other < scale.size();
    }

    private List<Pitch> withSteps(int position, List<Integer> steps) {
        return IntStream.concat(IntStream.of(0), steps.stream().mapToInt(Integer::intValue))
                .mapToObj(step -> scale.get(position + direction * step))
                .sorted()
                .toList();
    }
}
