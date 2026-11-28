package bg.petarh.piano.engine.playback;

import bg.petarh.piano.engine.run.SessionListener;
import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.time.Fraction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class PlaybackScheduler implements SessionListener {

    private final Session session;
    private final List<NoteEvent> notes;
    private final NoteOutput output;
    private final boolean gated;
    private final List<NoteEvent> sounding = new ArrayList<>();
    private int next;

    public PlaybackScheduler(Session session, List<NoteEvent> notes, NoteOutput output, boolean gated) {
        this.session = session;
        this.notes = notes.stream().sorted(Comparator.comparing(NoteEvent::start)).toList();
        this.output = output;
        this.gated = gated;
    }

    @Override
    public void onPosition(long positionNanos) {
        if (gated) {
            return;
        }
        Fraction now = session.positionAt(positionNanos);
        release(note -> note.end().compareTo(now) <= 0);
        while (next < notes.size() && notes.get(next).start().compareTo(now) <= 0) {
            play(notes.get(next++));
        }
    }

    @Override
    public void onTargetChanged(Optional<Target> target) {
        if (!gated) {
            return;
        }
        release(note -> true);
        while (next < notes.size()
                && target.map(reached -> notes.get(next).start().compareTo(reached.start()) < 0).orElse(true)) {
            play(notes.get(next++));
        }
    }

    @Override
    public void onStateChanged(SessionState state) {
        if (state == SessionState.PAUSED || state == SessionState.FINISHED) {
            sounding.clear();
            output.allOff();
        }
    }

    private void play(NoteEvent note) {
        output.noteOn(note.pitch());
        sounding.add(note);
    }

    private void release(Predicate<NoteEvent> finished) {
        for (Iterator<NoteEvent> iterator = sounding.iterator(); iterator.hasNext(); ) {
            NoteEvent note = iterator.next();
            if (finished.test(note)) {
                output.noteOff(note.pitch());
                iterator.remove();
            }
        }
    }
}
