package bg.petarh.piano.generator.chords;

import bg.petarh.piano.model.chord.Chord;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.time.Fraction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public final class RandomChordGenerator {

    private final ChordSettings settings;
    private final long seed;
    private final RandomGenerator random;
    private final List<Hand> handOrder;
    private final Map<Hand, List<Chord>> candidates = new EnumMap<>(Hand.class);
    private Optional<Chord> previous = Optional.empty();
    private long measureIndex;

    private RandomChordGenerator(ChordSettings settings, long seed) {
        this.settings = settings;
        this.seed = seed;
        this.random = new SplittableRandom(seed);
        this.handOrder = List.copyOf(settings.hands());
        for (Hand hand : handOrder) {
            candidates.put(hand, ChordCandidates.forHand(settings, hand));
        }
    }

    public static RandomChordGenerator withSeed(ChordSettings settings, long seed) {
        return new RandomChordGenerator(settings, seed);
    }

    public static RandomChordGenerator withRandomSeed(ChordSettings settings) {
        return new RandomChordGenerator(settings, ThreadLocalRandom.current().nextLong());
    }

    public long seed() {
        return seed;
    }

    public List<ChordEvent> nextMeasure() {
        Hand hand = handOrder.get((int) (measureIndex % handOrder.size()));
        Fraction duration = settings.noteValue().duration();
        Fraction start = settings.meter().measureLength().times(measureIndex);
        int count = settings.noteValue().notesPerMeasure(settings.meter());
        List<ChordEvent> events = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            events.add(new ChordEvent(nextChord(hand), start.plus(duration.times(i)), duration, hand));
        }
        measureIndex++;
        return List.copyOf(events);
    }

    private Chord nextChord(Hand hand) {
        List<Chord> choices = candidates.get(hand).stream()
                .filter(chord -> previous.map(last -> !last.equals(chord)).orElse(true))
                .toList();
        List<Chord> pool = choices.isEmpty() ? candidates.get(hand) : choices;
        Chord chord = pool.get(random.nextInt(pool.size()));
        previous = Optional.of(chord);
        return chord;
    }
}
