package bg.petarh.piano.model.score;

import bg.petarh.piano.model.time.Fraction;

public record Placed<T>(Fraction at, T value) {
}
