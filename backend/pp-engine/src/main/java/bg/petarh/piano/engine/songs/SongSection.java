package bg.petarh.piano.engine.songs;

import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.score.Placed;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TempoMap;

import java.util.ArrayList;
import java.util.List;

public record SongSection(int firstBar, int lastBar) {

    public static final int MAX_PASSES = 99;

    public SongSection {
        if (firstBar < 1 || lastBar < firstBar) {
            throw new IllegalArgumentException("Choose bars from 1 up, the first bar before the last: "
                    + firstBar + "–" + lastBar);
        }
    }

    public static SongSection whole(Score score) {
        return new SongSection(1, score.measureCount());
    }

    public SectionLoop loop(Score score, SongSelection selection, int passes) {
        List<Fraction> starts = score.measureStarts();
        if (lastBar > starts.size()) {
            throw new IllegalArgumentException("The song has " + starts.size() + " bars; bar " + lastBar
                    + " doesn't exist");
        }
        if (passes < 1 || passes > MAX_PASSES) {
            throw new IllegalArgumentException("Repeat a section 1 to " + MAX_PASSES + " times: " + passes);
        }
        Fraction start = starts.get(firstBar - 1);
        Fraction end = lastBar < starts.size() ? starts.get(lastBar) : score.length();
        Fraction length = end.minus(start);
        List<NoteEvent> played = repeated(within(selection.played(), start, end), length, passes);
        if (played.isEmpty()) {
            throw new IllegalArgumentException("Bars " + firstBar + "–" + lastBar + " have no notes for you to play");
        }
        return new SectionLoop(played, repeated(within(selection.accompaniment(), start, end), length, passes),
                tempoMap(score, start, end, length, passes), length, passes);
    }

    private static List<NoteEvent> within(List<NoteEvent> notes, Fraction start, Fraction end) {
        List<NoteEvent> kept = new ArrayList<>();
        for (NoteEvent note : notes) {
            if (note.start().compareTo(start) >= 0 && note.start().compareTo(end) < 0) {
                Fraction noteEnd = note.end().compareTo(end) > 0 ? end : note.end();
                Fraction moved = note.start().minus(start);
                kept.add(new NoteEvent(note.pitch(), moved, noteEnd.minus(note.start()), note.hand()));
            }
        }
        return kept;
    }

    private static List<NoteEvent> repeated(List<NoteEvent> notes, Fraction length, int passes) {
        List<NoteEvent> all = new ArrayList<>();
        for (int pass = 0; pass < passes; pass++) {
            Fraction offset = length.times(pass);
            for (NoteEvent note : notes) {
                all.add(new NoteEvent(note.pitch(), note.start().plus(offset), note.duration(), note.hand()));
            }
        }
        return all;
    }

    private static TempoMap tempoMap(Score score, Fraction start, Fraction end, Fraction length, int passes) {
        Tempo first = score.tempoMap().tempoAt(start);
        TempoMap map = TempoMap.constant(first);
        for (int pass = 0; pass < passes; pass++) {
            Fraction offset = length.times(pass);
            if (pass > 0) {
                map = map.withChangeAt(offset, first);
            }
            for (Placed<Tempo> change : score.tempos()) {
                if (change.at().compareTo(start) > 0 && change.at().compareTo(end) < 0) {
                    map = map.withChangeAt(change.at().minus(start).plus(offset), change.value());
                }
            }
        }
        return map;
    }
}
