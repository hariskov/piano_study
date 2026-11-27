package bg.petarh.piano.model.note;

import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.pitch.Pitch;

import java.util.Arrays;
import java.util.Optional;

public enum Hand {
    RIGHT(1, KeyboardRange.of(Pitch.parse("C4"), Pitch.of(127))),
    LEFT(2, KeyboardRange.of(Pitch.of(0), Pitch.parse("B3")));

    private final int staff;
    private final KeyboardRange sideOfMiddleC;

    Hand(int staff, KeyboardRange sideOfMiddleC) {
        this.staff = staff;
        this.sideOfMiddleC = sideOfMiddleC;
    }

    public int staff() {
        return staff;
    }

    public static Hand forStaff(int staff) {
        return Arrays.stream(values())
                .filter(hand -> hand.staff == staff)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No hand plays staff " + staff));
    }

    public Optional<KeyboardRange> portionOf(KeyboardRange range) {
        return range.intersect(sideOfMiddleC);
    }
}
