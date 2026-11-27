package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.NoteValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.random.RandomGenerator;

final class Rhythm {

    private Rhythm() {
    }

    static List<Fraction> fill(Fraction measure, NoteValue shortest, boolean mixed, RandomGenerator random) {
        List<Fraction> durations = new ArrayList<>();
        Fraction remaining = measure;
        while (remaining.isPositive()) {
            Fraction duration = mixed ? anyFitting(remaining, shortest, random) : shortest.duration();
            durations.add(duration);
            remaining = remaining.minus(duration);
        }
        return List.copyOf(durations);
    }

    private static Fraction anyFitting(Fraction remaining, NoteValue shortest, RandomGenerator random) {
        List<Fraction> fitting = Arrays.stream(NoteValue.values())
                .map(NoteValue::duration)
                .filter(duration -> duration.compareTo(shortest.duration()) >= 0)
                .filter(duration -> duration.compareTo(remaining) <= 0)
                .toList();
        return fitting.get(random.nextInt(fitting.size()));
    }
}
