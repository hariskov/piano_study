package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.chord.Chord;
import bg.petarh.piano.model.chord.ChordType;
import bg.petarh.piano.model.chord.Inversion;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.pitch.Pitch;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

final class Harmony {

    private static final int CHANCES = 4;
    private static final int SMALLEST_INTERVAL_STEPS = 2;
    private static final int LARGEST_INTERVAL_STEPS = 5;
    private static final List<Integer> TRIAD_STEPS = List.of(2, 4);

    private final List<Pitch> scale;
    private final KeyboardRange handRange;
    private final Hand hand;
    private final Level level;
    private final Set<ChordType> chordTypes;
    private final boolean accidentals;

    private Harmony(List<Pitch> scale, KeyboardRange handRange, Hand hand, Level level, Set<ChordType> chordTypes,
                    boolean accidentals) {
        this.scale = scale;
        this.handRange = handRange;
        this.hand = hand;
        this.level = level;
        this.chordTypes = chordTypes;
        this.accidentals = accidentals;
    }

    static Harmony of(List<Pitch> scale, KeyboardRange handRange, Hand hand, Level level, Set<ChordType> chordTypes,
                      boolean accidentals) {
        return new Harmony(scale, handRange, hand, level, chordTypes, accidentals);
    }

    List<Pitch> around(Pitch melody, RandomGenerator random) {
        int notesTogether = random.nextInt(CHANCES) + 2;
        if (notesTogether > level.mostNotesTogether()) {
            return List.of(melody);
        }
        if (notesTogether == 2) {
            return interval(melody, random);
        }
        return level.usesChordTypes() ? chordOfChosenType(melody, random) : triad(melody);
    }

    private List<Pitch> interval(Pitch melody, RandomGenerator random) {
        int position = scale.indexOf(melody);
        int[] fitting = IntStream.rangeClosed(SMALLEST_INTERVAL_STEPS, LARGEST_INTERVAL_STEPS)
                .filter(steps -> position >= 0 && fits(position, steps))
                .toArray();
        if (fitting.length == 0) {
            return List.of(melody);
        }
        return withSteps(position, List.of(fitting[random.nextInt(fitting.length)]));
    }

    private List<Pitch> triad(Pitch melody) {
        int position = scale.indexOf(melody);
        return position >= 0 && fits(position, TRIAD_STEPS.getLast())
                ? withSteps(position, TRIAD_STEPS)
                : List.of(melody);
    }

    private List<Pitch> chordOfChosenType(Pitch melody, RandomGenerator random) {
        List<List<Pitch>> fitting = chordTypes.stream()
                .flatMap(type -> Arrays.stream(Inversion.values()).filter(type::allows)
                        .map(inversion -> chordWithOuterNote(melody, type, inversion)))
                .filter(pitches -> !pitches.isEmpty())
                .filter(pitches -> accidentals || scale.containsAll(pitches))
                .toList();
        return fitting.isEmpty() ? List.of(melody) : fitting.get(random.nextInt(fitting.size()));
    }

    private List<Pitch> chordWithOuterNote(Pitch melody, ChordType type, Inversion inversion) {
        List<Integer> voicing = type.voicing(inversion);
        int root = melody.midiNumber() - (hand == Hand.RIGHT ? voicing.getLast() : voicing.getFirst());
        boolean fitsHand = voicing.stream().allMatch(semitones -> root + semitones >= handRange.lowest().midiNumber()
                && root + semitones <= handRange.highest().midiNumber());
        return root >= 0 && fitsHand ? Chord.of(Pitch.of(root), type, inversion).pitches() : List.of();
    }

    private boolean fits(int position, int steps) {
        int other = position + direction() * steps;
        return other >= 0 && other < scale.size();
    }

    private List<Pitch> withSteps(int position, List<Integer> steps) {
        return IntStream.concat(IntStream.of(0), steps.stream().mapToInt(Integer::intValue))
                .mapToObj(step -> scale.get(position + direction() * step))
                .sorted()
                .toList();
    }

    private int direction() {
        return hand == Hand.RIGHT ? -1 : 1;
    }
}
