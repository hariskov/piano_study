package bg.petarh.piano.engine.playback;

import bg.petarh.piano.model.note.NoteEvent;

import java.util.List;

public record Playback(List<NoteEvent> notes, NoteOutput output, boolean gated) {

    public Playback {
        notes = List.copyOf(notes);
    }
}
