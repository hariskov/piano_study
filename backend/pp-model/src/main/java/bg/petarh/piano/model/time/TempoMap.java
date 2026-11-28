package bg.petarh.piano.model.time;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TempoMap {

    private record Segment(Fraction start, long startNanos, Tempo tempo) {
    }

    private final List<Segment> segments;

    private TempoMap(List<Segment> segments) {
        this.segments = List.copyOf(segments);
    }

    public static TempoMap constant(Tempo tempo) {
        return new TempoMap(List.of(new Segment(Fraction.ZERO, 0, Objects.requireNonNull(tempo, "tempo"))));
    }

    public TempoMap withChangeAt(Fraction position, Tempo tempo) {
        if (position.compareTo(Fraction.ZERO) < 0) {
            throw new IllegalArgumentException("Tempo cannot change before the start: " + position);
        }
        List<Segment> kept = new ArrayList<>(segments.stream()
                .filter(segment -> segment.start().compareTo(position) < 0)
                .toList());
        long startNanos = kept.isEmpty() ? 0 : new TempoMap(kept).nanosAt(position);
        kept.add(new Segment(position, startNanos, tempo));
        return new TempoMap(kept);
    }

    public TempoMap scaledBy(double factor) {
        TempoMap scaled = constant(scaled(segments.getFirst().tempo(), factor));
        for (Segment segment : segments.subList(1, segments.size())) {
            scaled = scaled.withChangeAt(segment.start(), scaled(segment.tempo(), factor));
        }
        return scaled;
    }

    public TempoMap withSegmentsFrom(Fraction position, TempoMap other) {
        TempoMap combined = withChangeAt(position, other.tempoAt(position));
        for (Segment segment : other.segments) {
            if (segment.start().compareTo(position) > 0) {
                combined = combined.withChangeAt(segment.start(), segment.tempo());
            }
        }
        return combined;
    }

    public long nanosAt(Fraction position) {
        Segment segment = segmentAt(position);
        return segment.startNanos() + segment.tempo().nanosFor(position.minus(segment.start()));
    }

    public Fraction positionAt(long nanos) {
        Segment segment = segments.getFirst();
        for (Segment candidate : segments) {
            if (candidate.startNanos() <= nanos) {
                segment = candidate;
            }
        }
        return segment.start().plus(segment.tempo().durationOf(nanos - segment.startNanos()));
    }

    public Tempo tempoAt(Fraction position) {
        return segmentAt(position).tempo();
    }

    public Tempo latestTempo() {
        return segments.getLast().tempo();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TempoMap map && map.segments.equals(segments);
    }

    @Override
    public int hashCode() {
        return segments.hashCode();
    }

    private static Tempo scaled(Tempo tempo, double factor) {
        long bpm = Math.round(tempo.bpm() * factor);
        return Tempo.of(Math.clamp(bpm, Tempo.SLOWEST_BPM, Tempo.FASTEST_BPM), tempo.beatUnit());
    }

    private Segment segmentAt(Fraction position) {
        Segment found = segments.getFirst();
        for (Segment segment : segments) {
            if (segment.start().compareTo(position) <= 0) {
                found = segment;
            }
        }
        return found;
    }
}
