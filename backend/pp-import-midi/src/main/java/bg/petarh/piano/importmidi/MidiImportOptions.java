package bg.petarh.piano.importmidi;

import bg.petarh.piano.model.time.Fraction;

public record MidiImportOptions(Fraction grid, boolean triplets, int splitPoint) {

    private static final int MIDDLE_C = 60;
    private static final MidiImportOptions DEFAULTS = new MidiImportOptions(Fraction.of(1, 16), false, MIDDLE_C);

    public MidiImportOptions {
        if (!grid.isPositive()) {
            throw new IllegalArgumentException("The quantisation grid must be positive: " + grid);
        }
        if (splitPoint < 0 || splitPoint > 127) {
            throw new IllegalArgumentException("The split point must be a MIDI note 0-127: " + splitPoint);
        }
    }

    public static MidiImportOptions defaults() {
        return DEFAULTS;
    }

    public MidiImportOptions withGrid(Fraction value) {
        return new MidiImportOptions(value, triplets, splitPoint);
    }

    public MidiImportOptions withTriplets(boolean value) {
        return new MidiImportOptions(grid, value, splitPoint);
    }

    public MidiImportOptions withSplitPoint(int midiNote) {
        return new MidiImportOptions(grid, triplets, midiNote);
    }
}
