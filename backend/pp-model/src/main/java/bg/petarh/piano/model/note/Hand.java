package bg.petarh.piano.model.note;

import java.util.Arrays;

public enum Hand {
    RIGHT(1),
    LEFT(2);

    private final int staff;

    Hand(int staff) {
        this.staff = staff;
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
}
