package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.time.Fraction;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

public final class RandomNotesGenerator {

    private final RandomNotesSettings settings;
    private final long seed;
    private final RandomGenerator random;
    private final List<Hand> handOrder;
    private final Map<Hand, MelodicLine> lines = new EnumMap<>(Hand.class);
    private long measureIndex;

    private RandomNotesGenerator(RandomNotesSettings settings, long seed) {
        this.settings = settings;
        this.seed = seed;
        this.random = new SplittableRandom(seed);
        this.handOrder = List.copyOf(settings.hands());
        for (Hand hand : handOrder) {
            lines.put(hand, MelodicLine.of(HandPitches.forHand(settings, hand), hand, settings.level()));
        }
    }

    public static RandomNotesGenerator withSeed(RandomNotesSettings settings, long seed) {
        return new RandomNotesGenerator(settings, seed);
    }

    public static RandomNotesGenerator withRandomSeed(RandomNotesSettings settings) {
        return new RandomNotesGenerator(settings, ThreadLocalRandom.current().nextLong());
    }

    public long seed() {
        return seed;
    }

    public List<NoteEvent> nextMeasure() {
        Hand hand = handOrder.get((int) (measureIndex % handOrder.size()));
        MelodicLine line = lines.get(hand);
        Fraction duration = settings.noteValue().duration();
        Fraction start = settings.meter().measureLength().times(measureIndex);
        int count = settings.noteValue().notesPerMeasure(settings.meter());
        List<NoteEvent> notes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            notes.add(new NoteEvent(line.next(random), start.plus(duration.times(i)), duration, hand));
        }
        measureIndex++;
        return List.copyOf(notes);
    }
}
