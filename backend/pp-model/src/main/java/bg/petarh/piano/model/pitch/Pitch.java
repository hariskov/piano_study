package bg.petarh.piano.model.pitch;

public final class Pitch implements Comparable<Pitch> {

    static final int LOWEST_MIDI_NUMBER = 0;
    static final int HIGHEST_MIDI_NUMBER = 127;
    static final int SEMITONES_PER_OCTAVE = 12;

    private final int midiNumber;

    private Pitch(int midiNumber) {
        this.midiNumber = midiNumber;
    }

    public static Pitch of(int midiNumber) {
        if (midiNumber < LOWEST_MIDI_NUMBER || midiNumber > HIGHEST_MIDI_NUMBER) {
            throw new IllegalArgumentException("MIDI number out of range 0-127: " + midiNumber);
        }
        return new Pitch(midiNumber);
    }

    public static Pitch parse(String name) {
        return of(PitchNotation.toMidiNumber(name));
    }

    public int midiNumber() {
        return midiNumber;
    }

    public int pitchClass() {
        return midiNumber % SEMITONES_PER_OCTAVE;
    }

    public int octave() {
        return midiNumber / SEMITONES_PER_OCTAVE - 1;
    }

    public String name() {
        return PitchNotation.sharpName(pitchClass()) + octave();
    }

    public boolean isBlackKey() {
        return PitchNotation.isAccidental(pitchClass());
    }

    public Pitch transpose(int semitones) {
        return of(midiNumber + semitones);
    }

    @Override
    public int compareTo(Pitch other) {
        return Integer.compare(midiNumber, other.midiNumber);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Pitch pitch && pitch.midiNumber == midiNumber;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(midiNumber);
    }

    @Override
    public String toString() {
        return name();
    }
}
