package bg.petarh.piano.generator.chords;

import bg.petarh.piano.model.chord.Chord;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.time.Fraction;

import java.util.List;

public record ChordEvent(Chord chord, Fraction start, Fraction duration, Hand hand) {

    public List<NoteEvent> notes() {
        return chord.pitches().stream().map(pitch -> new NoteEvent(pitch, start, duration, hand)).toList();
    }
}
