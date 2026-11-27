package bg.petarh.piano.model.time;

public enum NoteValue {
    WHOLE(1),
    HALF(2),
    QUARTER(4),
    EIGHTH(8);

    private final Fraction duration;

    NoteValue(int perWholeNote) {
        this.duration = Fraction.of(1, perWholeNote);
    }

    public Fraction duration() {
        return duration;
    }

    public boolean fillsMeasureOf(TimeSignature meter) {
        return meter.measureLength().dividedBy(duration).denominator() == 1;
    }

    public int notesPerMeasure(TimeSignature meter) {
        if (!fillsMeasureOf(meter)) {
            throw new IllegalArgumentException(this + " notes don't fill a " + meter + " measure");
        }
        return Math.toIntExact(meter.measureLength().dividedBy(duration).numerator());
    }
}
