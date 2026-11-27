package bg.petarh.piano.importmidi;

import bg.petarh.piano.model.time.Fraction;

final class Quantiser {

    record Span(int pitch, Fraction start, Fraction end) {
    }

    private final long ticksPerWhole;
    private final Fraction grid;
    private final Fraction tripletGrid;
    private final boolean triplets;

    Quantiser(int ticksPerQuarter, MidiImportOptions options) {
        this.ticksPerWhole = 4L * ticksPerQuarter;
        this.grid = options.grid();
        this.tripletGrid = options.grid().times(Fraction.of(2, 3));
        this.triplets = options.triplets();
    }

    Fraction position(long tick) {
        Fraction exact = Fraction.of(tick, ticksPerWhole);
        Fraction straight = snap(exact, grid);
        if (!triplets) {
            return straight;
        }
        Fraction triplet = snap(exact, tripletGrid);
        return distance(triplet, exact).compareTo(distance(straight, exact)) < 0 ? triplet : straight;
    }

    Span place(int pitch, long startTick, long endTick) {
        Fraction start = position(startTick);
        Fraction end = position(endTick);
        if (end.compareTo(start) <= 0) {
            end = start.plus(triplets && isOnTripletGridOnly(start) ? tripletGrid : grid);
        }
        return new Span(pitch, start, end);
    }

    private boolean isOnTripletGridOnly(Fraction position) {
        return position.dividedBy(grid).denominator() != 1;
    }

    private static Fraction snap(Fraction exact, Fraction step) {
        Fraction steps = exact.dividedBy(step);
        long nearest = Math.floorDiv(2 * steps.numerator() + steps.denominator(), 2 * steps.denominator());
        return step.times(nearest);
    }

    private static Fraction distance(Fraction a, Fraction b) {
        Fraction difference = a.minus(b);
        return difference.compareTo(Fraction.ZERO) < 0 ? difference.times(-1) : difference;
    }
}
