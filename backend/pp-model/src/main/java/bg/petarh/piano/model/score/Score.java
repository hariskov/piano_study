package bg.petarh.piano.model.score;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TempoMap;
import bg.petarh.piano.model.time.TimeSignature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class Score {

    private static final Tempo DEFAULT_TEMPO = Tempo.of(120);

    private final String title;
    private final String composer;
    private final List<Part> parts;
    private final List<Placed<TimeSignature>> timeSignatures;
    private final List<Placed<KeySignature>> keys;
    private final List<Placed<Tempo>> tempos;
    private final Fraction length;
    private final List<Fraction> givenMeasureStarts;

    private Score(Builder builder) {
        this.title = builder.title;
        this.composer = builder.composer;
        this.parts = List.copyOf(builder.parts);
        this.length = builder.length;
        this.timeSignatures = placed(builder.timeSignatures, TimeSignature.COMMON_TIME, length);
        this.keys = placed(builder.keys, KeySignature.major("C"), length);
        this.tempos = placed(builder.tempos, DEFAULT_TEMPO, length);
        this.givenMeasureStarts = checkedMeasureStarts(builder.measureStarts);
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    public String title() {
        return title;
    }

    public Optional<String> composer() {
        return Optional.ofNullable(composer);
    }

    public List<Part> parts() {
        return parts;
    }

    public Optional<Part> part(String id) {
        return parts.stream().filter(part -> part.id().equals(id)).findFirst();
    }

    public Fraction length() {
        return length;
    }

    public List<Placed<TimeSignature>> timeSignatures() {
        return timeSignatures;
    }

    public List<Placed<KeySignature>> keys() {
        return keys;
    }

    public List<Placed<Tempo>> tempos() {
        return tempos;
    }

    public TimeSignature timeSignatureAt(Fraction position) {
        return valueAt(timeSignatures, position);
    }

    public KeySignature keyAt(Fraction position) {
        return valueAt(keys, position);
    }

    public TempoMap tempoMap() {
        TempoMap map = TempoMap.constant(tempos.getFirst().value());
        for (Placed<Tempo> change : tempos.subList(1, tempos.size())) {
            map = map.withChangeAt(change.at(), change.value());
        }
        return map;
    }

    public List<Fraction> measureStarts() {
        if (!givenMeasureStarts.isEmpty()) {
            return givenMeasureStarts;
        }
        List<Fraction> starts = new ArrayList<>();
        for (Fraction at = Fraction.ZERO; at.compareTo(length) < 0; at = at.plus(timeSignatureAt(at).measureLength())) {
            starts.add(at);
        }
        return List.copyOf(starts);
    }

    public int measureCount() {
        return measureStarts().size();
    }

    public int measureIndexAt(Fraction position) {
        List<Fraction> starts = measureStarts();
        int index = 0;
        while (index + 1 < starts.size() && starts.get(index + 1).compareTo(position) <= 0) {
            index++;
        }
        return index;
    }

    private static List<Fraction> checkedMeasureStarts(List<Fraction> starts) {
        if (starts.isEmpty()) {
            return List.of();
        }
        if (!starts.getFirst().isZero()) {
            throw new IllegalArgumentException("The first measure must start at 0, not " + starts.getFirst());
        }
        for (int i = 1; i < starts.size(); i++) {
            if (starts.get(i).compareTo(starts.get(i - 1)) <= 0) {
                throw new IllegalArgumentException("Measure starts must go forward: " + starts);
            }
        }
        return List.copyOf(starts);
    }

    private static <T> T valueAt(List<Placed<T>> changes, Fraction position) {
        T value = changes.getFirst().value();
        for (Placed<T> change : changes) {
            if (change.at().compareTo(position) <= 0) {
                value = change.value();
            }
        }
        return value;
    }

    private static <T> List<Placed<T>> placed(List<Placed<T>> given, T fallback, Fraction length) {
        List<Placed<T>> sorted = new ArrayList<>(given);
        sorted.sort(Comparator.comparing(Placed::at));
        for (Placed<T> change : sorted) {
            if (change.at().compareTo(Fraction.ZERO) > 0 && change.at().compareTo(length) >= 0) {
                throw new IllegalArgumentException("Change at " + change.at() + " is past the end of the score (" + length + ")");
            }
        }
        if (sorted.isEmpty() || sorted.getFirst().at().isPositive()) {
            sorted.addFirst(new Placed<>(Fraction.ZERO, fallback));
        }
        return List.copyOf(sorted);
    }

    public static final class Builder {

        private final String title;
        private String composer;
        private final List<Part> parts = new ArrayList<>();
        private final Set<String> partIds = new HashSet<>();
        private final List<Placed<TimeSignature>> timeSignatures = new ArrayList<>();
        private final List<Placed<KeySignature>> keys = new ArrayList<>();
        private final List<Placed<Tempo>> tempos = new ArrayList<>();
        private Fraction length = Fraction.ZERO;
        private List<Fraction> measureStarts = List.of();

        private Builder(String title) {
            this.title = Objects.requireNonNull(title, "title");
        }

        public Builder composer(String name) {
            this.composer = name;
            return this;
        }

        public Builder part(Part part) {
            if (!partIds.add(part.id())) {
                throw new IllegalArgumentException("Two parts have the id " + part.id());
            }
            parts.add(part);
            return this;
        }

        public Builder timeSignature(Fraction at, TimeSignature signature) {
            timeSignatures.add(new Placed<>(at, signature));
            return this;
        }

        public Builder key(Fraction at, KeySignature key) {
            keys.add(new Placed<>(at, key));
            return this;
        }

        public Builder tempo(Fraction at, Tempo tempo) {
            tempos.add(new Placed<>(at, tempo));
            return this;
        }

        public Builder measureStarts(List<Fraction> starts) {
            this.measureStarts = List.copyOf(starts);
            return this;
        }

        public Builder length(Fraction scoreLength) {
            this.length = scoreLength;
            return this;
        }

        public Score build() {
            return new Score(this);
        }
    }
}
