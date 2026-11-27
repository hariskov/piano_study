package bg.petarh.piano.model.chord;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.pitch.Pitch;

import java.util.List;
import java.util.Objects;

public final class Chord {

    private static final int HIGHEST_MIDI_NUMBER = 127;

    private final Pitch root;
    private final ChordType type;
    private final Inversion inversion;
    private final List<Pitch> pitches;

    private Chord(Pitch root, ChordType type, Inversion inversion, List<Pitch> pitches) {
        this.root = root;
        this.type = type;
        this.inversion = inversion;
        this.pitches = pitches;
    }

    public static Chord of(Pitch root, ChordType type, Inversion inversion) {
        Objects.requireNonNull(root, "root");
        List<Integer> semitones = type.voicing(inversion);
        if (root.midiNumber() + semitones.getLast() > HIGHEST_MIDI_NUMBER) {
            throw new IllegalArgumentException("Chord on " + root + " goes above the highest MIDI note");
        }
        return new Chord(root, type, inversion, semitones.stream().map(root::transpose).toList());
    }

    public Pitch root() {
        return root;
    }

    public ChordType type() {
        return type;
    }

    public Inversion inversion() {
        return inversion;
    }

    public List<Pitch> pitches() {
        return pitches;
    }

    public Pitch bass() {
        return pitches.getFirst();
    }

    public String name(KeySignature key) {
        ChordSpelling spelling = ChordSpelling.forRoot(root.pitchClass(), key.usesFlats());
        String name = spelling.toneName(0, 0) + type.symbol();
        if (inversion == Inversion.ROOT_POSITION) {
            return name;
        }
        int bassStep = inversion.number();
        return name + "/" + spelling.toneName(bassStep, type.semitonesAboveRoot().get(bassStep));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Chord chord
                && chord.root.equals(root)
                && chord.type == type
                && chord.inversion == inversion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(root, type, inversion);
    }

    @Override
    public String toString() {
        return name(KeySignature.major("C")) + " " + pitches;
    }
}
