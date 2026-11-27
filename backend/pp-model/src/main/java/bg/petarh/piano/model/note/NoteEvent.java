package bg.petarh.piano.model.note;

import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;

import java.util.Objects;

public record NoteEvent(Pitch pitch, Fraction start, Fraction duration, Hand hand) {

    public NoteEvent {
        Objects.requireNonNull(pitch, "pitch");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(duration, "duration");
        Objects.requireNonNull(hand, "hand");
        if (start.compareTo(Fraction.ZERO) < 0) {
            throw new IllegalArgumentException("Note cannot start before the beginning: " + start);
        }
        if (!duration.isPositive()) {
            throw new IllegalArgumentException("Note duration must be positive: " + duration);
        }
    }

    public Fraction end() {
        return start.plus(duration);
    }
}
