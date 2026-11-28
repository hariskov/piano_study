package bg.petarh.piano.engine.songs;

import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.TempoMap;

import java.util.List;

public record SectionLoop(List<NoteEvent> played, List<NoteEvent> accompaniment, TempoMap tempoMap,
                          Fraction passLength, int passes) {

    public SectionLoop {
        played = List.copyOf(played);
        accompaniment = List.copyOf(accompaniment);
    }

    public Fraction length() {
        return passLength.times(passes);
    }

    public int passAt(Fraction position) {
        int pass = (int) position.dividedBy(passLength).floor() + 1;
        return Math.clamp(pass, 1, passes);
    }
}
