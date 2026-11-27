package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.pitch.Pitch;

import java.util.List;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;

final class MelodicLine {

    private static final int SMALL_RANGE_NOTES = 8;
    private static final int LARGEST_STEP = 2;
    private static final int LARGEST_JUMP_SEMITONES = 12;

    private final List<Pitch> pitches;
    private final Level level;
    private int position = -1;

    private MelodicLine(List<Pitch> pitches, Level level) {
        this.pitches = pitches;
        this.level = level;
    }

    static MelodicLine of(List<Pitch> handPitches, Hand hand, Level level) {
        return new MelodicLine(level == Level.ONE ? nearMiddle(handPitches, hand) : handPitches, level);
    }

    Pitch next(RandomGenerator random) {
        int[] candidates = position < 0
                ? IntStream.range(0, pitches.size()).toArray()
                : IntStream.range(0, pitches.size()).filter(this::isReachable).toArray();
        position = candidates[random.nextInt(candidates.length)];
        return pitches.get(position);
    }

    private boolean isReachable(int candidate) {
        if (candidate == position) {
            return false;
        }
        return switch (level) {
            case ONE -> Math.abs(candidate - position) <= LARGEST_STEP;
            case TWO -> Math.abs(pitches.get(candidate).midiNumber() - pitches.get(position).midiNumber())
                    <= LARGEST_JUMP_SEMITONES;
        };
    }

    private static List<Pitch> nearMiddle(List<Pitch> pitches, Hand hand) {
        int size = Math.min(SMALL_RANGE_NOTES, pitches.size());
        return hand == Hand.RIGHT ? pitches.subList(0, size) : pitches.subList(pitches.size() - size, pitches.size());
    }
}
