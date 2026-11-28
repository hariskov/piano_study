package bg.petarh.piano.importmidi;

import bg.petarh.piano.model.time.Fraction;

import java.util.Set;

/**
 * {@code excludedParts} names parts by the ids a conversion with the same other options gives them
 * ("P1", "P2", …), so a preview's ids can be used to leave parts out of the import.
 */
public record MidiImportOptions(Fraction grid, boolean triplets, int splitPoint, Set<String> excludedParts) {

    private static final int MIDDLE_C = 60;
    private static final MidiImportOptions DEFAULTS = new MidiImportOptions(Fraction.of(1, 16), false, MIDDLE_C, Set.of());

    public MidiImportOptions {
        if (!grid.isPositive()) {
            throw new IllegalArgumentException("The quantisation grid must be positive: " + grid);
        }
        if (splitPoint < 0 || splitPoint > 127) {
            throw new IllegalArgumentException("The split point must be a MIDI note 0-127: " + splitPoint);
        }
        excludedParts = Set.copyOf(excludedParts);
    }

    public static MidiImportOptions defaults() {
        return DEFAULTS;
    }

    public MidiImportOptions withGrid(Fraction value) {
        return new MidiImportOptions(value, triplets, splitPoint, excludedParts);
    }

    public MidiImportOptions withTriplets(boolean value) {
        return new MidiImportOptions(grid, value, splitPoint, excludedParts);
    }

    public MidiImportOptions withSplitPoint(int midiNote) {
        return new MidiImportOptions(grid, triplets, midiNote, excludedParts);
    }

    public MidiImportOptions withExcludedParts(Set<String> partIds) {
        return new MidiImportOptions(grid, triplets, splitPoint, partIds);
    }
}
