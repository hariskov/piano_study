package io.github.hariskov.piano.midi;

final class MidiRanges {

    private MidiRanges() {
    }

    static void requireAudibleVelocity(int velocity) {
        if (velocity < 1 || velocity > 127) {
            throw new IllegalArgumentException("Velocity out of range 1-127: " + velocity);
        }
    }

    static void requireChannel(int channel) {
        if (channel < 0 || channel > 15) {
            throw new IllegalArgumentException("Channel out of range 0-15: " + channel);
        }
    }
}
