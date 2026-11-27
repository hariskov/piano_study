package bg.petarh.piano.midi.input;

@FunctionalInterface
public interface TimestampSource {

    long nanos();

    static TimestampSource system() {
        return System::nanoTime;
    }
}
