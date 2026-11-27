package bg.petarh.piano.engine.evaluation;

import bg.petarh.piano.engine.evaluation.Evaluation.CorrectNote;
import bg.petarh.piano.engine.evaluation.Evaluation.TargetCompleted;
import bg.petarh.piano.engine.evaluation.Evaluation.WrongNote;
import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WaitEvaluator implements Evaluator {

    public static final Duration DEFAULT_CHORD_WINDOW = Duration.ofMillis(300);

    private final Session session;
    private final long chordWindowNanos;
    private final Set<Pitch> held = new HashSet<>();
    private final Map<Pitch, Long> countedAt = new HashMap<>();
    private int completedTargets;
    private int wrongKeys;

    public WaitEvaluator(Session session) {
        this(session, DEFAULT_CHORD_WINDOW);
    }

    public WaitEvaluator(Session session, Duration chordWindow) {
        if (chordWindow.isNegative()) {
            throw new IllegalArgumentException("Chord window cannot be negative: " + chordWindow);
        }
        this.session = session;
        this.chordWindowNanos = chordWindow.toNanos();
    }

    @Override
    public List<Evaluation> onInput(InputEvent event) {
        return switch (event) {
            case InputEvent.NoteOn noteOn -> pressed(noteOn.pitch(), noteOn.timestampNanos());
            case InputEvent.NoteOff noteOff -> {
                held.remove(noteOff.pitch());
                yield List.of();
            }
            case InputEvent.SustainPedal ignored -> List.of();
        };
    }

    public int completedTargets() {
        return completedTargets;
    }

    public int wrongKeys() {
        return wrongKeys;
    }

    private List<Evaluation> pressed(Pitch pitch, long at) {
        held.add(pitch);
        if (session.state() != SessionState.RUNNING) {
            return List.of();
        }
        Target target = session.currentTarget().orElseThrow();
        if (!target.pitches().contains(pitch)) {
            wrongKeys++;
            return List.of(new WrongNote(pitch, at));
        }
        List<Evaluation> results = new ArrayList<>(2);
        if (countedAt.put(pitch, at) == null) {
            results.add(new CorrectNote(pitch, at));
        }
        if (isComplete(target)) {
            results.add(new TargetCompleted(target, at));
            completedTargets++;
            countedAt.clear();
            session.advance();
        }
        return List.copyOf(results);
    }

    private boolean isComplete(Target target) {
        if (!countedAt.keySet().containsAll(target.pitches())) {
            return false;
        }
        long spread = Collections.max(countedAt.values()) - Collections.min(countedAt.values());
        return held.containsAll(target.pitches()) || spread <= chordWindowNanos;
    }
}
