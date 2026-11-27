package bg.petarh.piano.model.score;

import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;

import java.util.Objects;

public record ScoreNote(Spelling spelling, Fraction start, Fraction duration, int staff, int voice, Tie tie) {

    private static final int MOST_STAVES = 2;

    public ScoreNote {
        Objects.requireNonNull(spelling, "spelling");
        Objects.requireNonNull(tie, "tie");
        if (start.compareTo(Fraction.ZERO) < 0) {
            throw new IllegalArgumentException("Note cannot start before the beginning: " + start);
        }
        if (!duration.isPositive()) {
            throw new IllegalArgumentException("Note duration must be positive: " + duration);
        }
        if (staff < 1 || staff > MOST_STAVES) {
            throw new IllegalArgumentException("Staff must be 1 or 2: " + staff);
        }
        if (voice < 1) {
            throw new IllegalArgumentException("Voice must be 1 or more: " + voice);
        }
    }

    public Pitch pitch() {
        return spelling.pitch();
    }

    public Fraction end() {
        return start.plus(duration);
    }
}
