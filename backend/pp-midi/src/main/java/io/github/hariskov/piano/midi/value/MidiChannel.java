package io.github.hariskov.piano.midi.value;

public final class MidiChannel {

    private static final int CHANNEL_COUNT = 16;

    private final int index;

    private MidiChannel(int index) {
        this.index = index;
    }

    public static MidiChannel of(int index) {
        if (index < 0 || index >= CHANNEL_COUNT) {
            throw new IllegalArgumentException("Channel index out of range 0-15: " + index);
        }
        return new MidiChannel(index);
    }

    public int index() {
        return index;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MidiChannel channel && channel.index == index;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(index);
    }

    @Override
    public String toString() {
        return "Channel " + (index + 1);
    }
}
