package io.github.hariskov.piano.midi;

@FunctionalInterface
public interface TimestampSource {

    long nanos();

    static TimestampSource system() {
        return System::nanoTime;
    }
}
