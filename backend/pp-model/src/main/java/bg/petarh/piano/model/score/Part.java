package bg.petarh.piano.model.score;

import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.pitch.Pitch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record Part(String id, String name, int staffCount, List<ScoreNote> notes) {

    private record Voice(Pitch pitch, int staff, int voice) {
    }

    public Part {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        if (staffCount < 1 || staffCount > 2) {
            throw new IllegalArgumentException("A part has 1 or 2 staves: " + staffCount);
        }
        notes = List.copyOf(notes);
        for (ScoreNote note : notes) {
            if (note.staff() > staffCount) {
                throw new IllegalArgumentException("Part " + id + " has " + staffCount + " staff, but a note is on staff "
                        + note.staff());
            }
        }
    }

    public List<NoteEvent> playableNotes() {
        List<NoteEvent> playable = new ArrayList<>();
        Map<Voice, NoteEvent> openTies = new HashMap<>();
        for (ScoreNote note : notes.stream().sorted(Comparator.comparing(ScoreNote::start)).toList()) {
            Voice voice = new Voice(note.pitch(), note.staff(), note.voice());
            NoteEvent open = openTies.get(voice);
            boolean continues = open != null && open.end().equals(note.start())
                    && (note.tie() == Tie.CONTINUE || note.tie() == Tie.STOP);
            if (continues) {
                NoteEvent longer = new NoteEvent(open.pitch(), open.start(), open.duration().plus(note.duration()),
                        open.hand());
                if (note.tie() == Tie.STOP) {
                    openTies.remove(voice);
                    playable.add(longer);
                } else {
                    openTies.put(voice, longer);
                }
                continue;
            }
            if (open != null) {
                playable.add(openTies.remove(voice));
            }
            NoteEvent event = new NoteEvent(note.pitch(), note.start(), note.duration(), handOf(note));
            if (note.tie() == Tie.START || note.tie() == Tie.CONTINUE) {
                openTies.put(voice, event);
            } else {
                playable.add(event);
            }
        }
        playable.addAll(openTies.values());
        playable.sort(Comparator.comparing(NoteEvent::start).thenComparing(NoteEvent::pitch));
        return List.copyOf(playable);
    }

    private static Hand handOf(ScoreNote note) {
        return note.staff() == 2 ? Hand.LEFT : Hand.RIGHT;
    }
}
