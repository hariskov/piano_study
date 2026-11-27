package bg.petarh.piano.midi.value;

public final class Velocity {

    private static final int SOFTEST = 1;
    private static final int LOUDEST = 127;

    private final int value;

    private Velocity(int value) {
        this.value = value;
    }

    public static Velocity of(int value) {
        if (value < SOFTEST || value > LOUDEST) {
            throw new IllegalArgumentException("Velocity out of range 1-127: " + value);
        }
        return new Velocity(value);
    }

    public int value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Velocity velocity && velocity.value == value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return "Velocity " + value;
    }
}
