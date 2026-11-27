package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;

import java.util.ArrayList;
import java.util.Comparator;
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
    private final Map<Hand, Harmony> harmonies = new EnumMap<>(Hand.class);
    private long measureIndex;

    private RandomNotesGenerator(RandomNotesSettings settings, long seed) {
        this.settings = settings;
        this.seed = seed;
        this.random = new SplittableRandom(seed);
        this.handOrder = List.copyOf(settings.hands());
        for (Hand hand : handOrder) {
            List<Pitch> scale = HandPitches.forHand(settings, hand);
            List<Pitch> allPitches = HandPitches.allForHand(settings, hand);
            lines.put(hand, MelodicLine.of(settings.playsAccidentals() ? allPitches : scale, hand, settings.level()));
            KeyboardRange handRange = KeyboardRange.of(allPitches.getFirst(), allPitches.getLast());
            harmonies.put(hand, Harmony.of(scale, handRange, hand, settings.level(),
                    settings.options().chordTypes(), settings.playsAccidentals()));
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
        Fraction start = settings.meter().measureLength().times(measureIndex);
        List<Hand> playing = settings.level().playsHandsTogether()
                ? handOrder
                : List.of(handOrder.get((int) (measureIndex % handOrder.size())));
        measureIndex++;
        return playing.stream()
                .flatMap(hand -> measureFor(hand, start).stream())
                .sorted(Comparator.comparing(NoteEvent::start).thenComparing(NoteEvent::pitch))
                .toList();
    }

    private List<NoteEvent> measureFor(Hand hand, Fraction start) {
        List<Fraction> durations = Rhythm.fill(settings.meter().measureLength(), settings.noteValue(),
                settings.playsMixedNoteValues(), random);
        List<NoteEvent> notes = new ArrayList<>();
        Fraction at = start;
        for (Fraction duration : durations) {
            Pitch melody = lines.get(hand).next(random);
            for (Pitch pitch : harmonies.get(hand).around(melody, random)) {
                notes.add(new NoteEvent(pitch, at, duration, hand));
            }
            at = at.plus(duration);
        }
        return notes;
    }
}
