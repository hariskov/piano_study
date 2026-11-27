package bg.petarh.piano.importmidi;

import bg.petarh.piano.model.time.Fraction;

public record MidiImportOptions(Fraction grid, boolean triplets) {

    private static final MidiImportOptions DEFAULTS = new MidiImportOptions(Fraction.of(1, 16), false);

    public MidiImportOptions {
        if (!grid.isPositive()) {
            throw new IllegalArgumentException("The quantisation grid must be positive: " + grid);
        }
    }

    public static MidiImportOptions defaults() {
        return DEFAULTS;
    }

    public MidiImportOptions withGrid(Fraction value) {
        return new MidiImportOptions(value, triplets);
    }

    public MidiImportOptions withTriplets(boolean value) {
        return new MidiImportOptions(grid, value);
    }
}
