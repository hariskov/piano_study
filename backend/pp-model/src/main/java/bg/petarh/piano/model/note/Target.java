package bg.petarh.piano.model.note;

import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class Target {

    private static final Comparator<NoteEvent> LOW_TO_HIGH =
            Comparator.comparing(NoteEvent::pitch).thenComparing(NoteEvent::hand);

    private final Fraction start;
    private final List<NoteEvent> notes;

    private Target(Fraction start, List<NoteEvent> notes) {
        this.start = start;
        this.notes = notes;
    }

    public static Target of(Collection<NoteEvent> notes) {
        if (notes.isEmpty()) {
            throw new IllegalArgumentException("A target needs at least one note");
        }
        Fraction start = notes.iterator().next().start();
        if (notes.stream().anyMatch(note -> !note.start().equals(start))) {
            throw new IllegalArgumentException("All notes of a target must start together: " + notes);
        }
        return new Target(start, notes.stream().sorted(LOW_TO_HIGH).toList());
    }

    public Fraction start() {
        return start;
    }

    public Fraction duration() {
        return notes.stream().map(NoteEvent::duration).max(Comparator.naturalOrder()).orElseThrow();
    }

    public List<NoteEvent> notes() {
        return notes;
    }

    public SortedSet<Pitch> pitches() {
        return pitchesOf(notes);
    }

    public SortedSet<Pitch> pitchesFor(Hand hand) {
        return pitchesOf(notes.stream().filter(note -> note.hand() == hand).toList());
    }

    public Set<Hand> hands() {
        return notes.stream().map(NoteEvent::hand).collect(Collectors.toCollection(() -> EnumSet.noneOf(Hand.class)));
    }

    public boolean isChord() {
        return pitches().size() > 1;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Target target && target.notes.equals(notes);
    }

    @Override
    public int hashCode() {
        return notes.hashCode();
    }

    @Override
    public String toString() {
        return pitches() + " at " + start;
    }

    private static SortedSet<Pitch> pitchesOf(List<NoteEvent> notes) {
        return notes.stream()
                .map(NoteEvent::pitch)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(TreeSet::new),
                        Collections::unmodifiableSortedSet));
    }
}
