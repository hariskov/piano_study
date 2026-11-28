package bg.petarh.piano.midi.value;

/** A General MIDI program: 0 is the acoustic grand piano, 19 the church organ, 56 the trumpet, and so on. */
public final class Instrument {

    public static final Instrument GRAND_PIANO = new Instrument(0);

    private static final int LAST = 127;

    private final int program;

    private Instrument(int program) {
        this.program = program;
    }

    public static Instrument of(int program) {
        if (program < 0 || program > LAST) {
            throw new IllegalArgumentException("Instrument out of range 0-127: " + program);
        }
        return new Instrument(program);
    }

    public int program() {
        return program;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Instrument instrument && instrument.program == program;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(program);
    }

    @Override
    public String toString() {
        return "Instrument " + program;
    }
}
