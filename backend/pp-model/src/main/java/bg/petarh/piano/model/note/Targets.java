package bg.petarh.piano.model.note;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class Targets {

    private Targets() {
    }

    public static List<Target> from(Collection<NoteEvent> notes) {
        return from(notes, EnumSet.allOf(Hand.class));
    }

    public static List<Target> from(Collection<NoteEvent> notes, Set<Hand> hands) {
        return notes.stream()
                .filter(note -> hands.contains(note.hand()))
                .collect(Collectors.groupingBy(NoteEvent::start, TreeMap::new, Collectors.toList()))
                .values().stream()
                .map(Target::of)
                .toList();
    }
}
