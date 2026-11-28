package bg.petarh.piano.engine.songs;

import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record SongSelection(List<NoteEvent> played, List<NoteEvent> accompaniment, int shifted, int skipped) {

    private static final int OCTAVE = 12;

    public SongSelection {
        played = List.copyOf(played);
        accompaniment = List.copyOf(accompaniment);
    }

    public static SongSelection of(Score score, String partId, Set<Hand> hands, KeyboardRange keyboard,
                                   OutOfRange outOfRange) {
        return of(score, partId, hands, keyboard, outOfRange, true);
    }

    /** With {@code playOtherHand} false, the hand you don't play is left out instead of accompanying you. */
    public static SongSelection of(Score score, String partId, Set<Hand> hands, KeyboardRange keyboard,
                                   OutOfRange outOfRange, boolean playOtherHand) {
        if (hands.isEmpty()) {
            throw new IllegalArgumentException("Choose at least one hand");
        }
        Part part = score.part(partId).orElseThrow(() -> new IllegalArgumentException("The song has no part " + partId));
        List<NoteEvent> played = new ArrayList<>();
        List<NoteEvent> accompaniment = new ArrayList<>();
        int shifted = 0;
        int skipped = 0;
        for (NoteEvent note : part.playableNotes()) {
            if (!hands.contains(note.hand())) {
                if (playOtherHand) {
                    accompaniment.add(note);
                }
                continue;
            }
            Optional<NoteEvent> fitted = fitted(note, keyboard, outOfRange);
            if (fitted.isEmpty()) {
                skipped++;
            } else {
                shifted += fitted.get().equals(note) ? 0 : 1;
                played.add(fitted.get());
            }
        }
        if (played.isEmpty() && skipped == 0) {
            throw new IllegalArgumentException(part.name() + " has no notes for the chosen hands");
        }
        for (Part other : score.parts()) {
            if (!other.id().equals(partId)) {
                accompaniment.addAll(other.playableNotes());
            }
        }
        accompaniment.sort(Comparator.comparing(NoteEvent::start).thenComparing(NoteEvent::pitch));
        return new SongSelection(played, accompaniment, shifted, skipped);
    }

    private static Optional<NoteEvent> fitted(NoteEvent note, KeyboardRange keyboard, OutOfRange outOfRange) {
        if (keyboard.contains(note.pitch())) {
            return Optional.of(note);
        }
        if (outOfRange == OutOfRange.SKIP) {
            return Optional.empty();
        }
        int midi = note.pitch().midiNumber();
        int step = midi < keyboard.lowest().midiNumber() ? OCTAVE : -OCTAVE;
        while (midi >= 0 && midi <= 127 && !keyboard.contains(Pitch.of(midi))) {
            midi += step;
            boolean passed = step > 0 ? midi > keyboard.highest().midiNumber() : midi < keyboard.lowest().midiNumber();
            if (passed) {
                return Optional.empty();
            }
        }
        return Optional.of(new NoteEvent(Pitch.of(midi), note.start(), note.duration(), note.hand()));
    }
}
