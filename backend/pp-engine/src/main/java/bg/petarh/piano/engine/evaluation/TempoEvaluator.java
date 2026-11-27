package bg.petarh.piano.engine.evaluation;

import bg.petarh.piano.engine.evaluation.Evaluation.MissedNote;
import bg.petarh.piano.engine.evaluation.Evaluation.TimedNote;
import bg.petarh.piano.engine.evaluation.Evaluation.WrongNote;
import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TempoEvaluator implements Evaluator {

    private static final int PEEK_STEP = 8;

    private final Session session;
    private final TimingWindows windows;
    private final Set<NoteEvent> matched = new HashSet<>();
    private final Map<TimingGrade, Integer> grades = new EnumMap<>(TimingGrade.class);
    private int misses;
    private int wrongKeys;

    public TempoEvaluator(Session session) {
        this(session, TimingWindows.DEFAULT);
    }

    public TempoEvaluator(Session session, TimingWindows windows) {
        this.session = session;
        this.windows = windows;
    }

    @Override
    public List<Evaluation> onInput(InputEvent event) {
        return switch (event) {
            case InputEvent.NoteOn noteOn -> pressed(noteOn.pitch(), noteOn.timestampNanos());
            case InputEvent.NoteOff ignored -> List.of();
            case InputEvent.SustainPedal ignored -> List.of();
        };
    }

    @Override
    public List<Evaluation> onTick(long nowNanos) {
        List<Evaluation> results = new ArrayList<>();
        long time = session.runningNanosAt(nowNanos);
        while (session.state() == SessionState.RUNNING) {
            Target current = session.currentTarget().orElseThrow();
            if (time <= expectedNanos(current.start()) + windows.ok().toNanos()) {
                break;
            }
            for (NoteEvent note : current.notes()) {
                if (!matched.remove(note)) {
                    misses++;
                    results.add(new MissedNote(note, nowNanos));
                }
            }
            session.advance();
        }
        return List.copyOf(results);
    }

    public int gradeCount(TimingGrade grade) {
        return grades.getOrDefault(grade, 0);
    }

    public int misses() {
        return misses;
    }

    public int wrongKeys() {
        return wrongKeys;
    }

    private List<Evaluation> pressed(Pitch pitch, long at) {
        if (session.state() != SessionState.RUNNING) {
            return List.of();
        }
        long time = session.runningNanosAt(at);
        Optional<NoteEvent> nearest = notesAround(time).stream()
                .filter(note -> note.pitch().equals(pitch) && !matched.contains(note))
                .filter(note -> windows.grade(offset(note, time)).isPresent())
                .min(Comparator.comparingLong(note -> Math.abs(offset(note, time))));
        if (nearest.isEmpty()) {
            wrongKeys++;
            return List.of(new WrongNote(pitch, at));
        }
        NoteEvent note = nearest.get();
        long offset = offset(note, time);
        TimingGrade grade = windows.grade(offset).orElseThrow();
        matched.add(note);
        grades.merge(grade, 1, Integer::sum);
        return List.of(new TimedNote(note, grade, offset, at));
    }

    private List<NoteEvent> notesAround(long time) {
        List<Target> candidates = new ArrayList<>();
        session.currentTarget().ifPresent(candidates::add);
        long latestStart = time + windows.ok().toNanos();
        int count = PEEK_STEP;
        List<Target> upcoming = session.upcoming(count);
        while (upcoming.size() == count && expectedNanos(upcoming.getLast().start()) <= latestStart) {
            count *= 2;
            upcoming = session.upcoming(count);
        }
        upcoming.stream().takeWhile(target -> expectedNanos(target.start()) <= latestStart).forEach(candidates::add);
        return candidates.stream().flatMap(target -> target.notes().stream()).toList();
    }

    private long offset(NoteEvent note, long time) {
        return time - expectedNanos(note.start());
    }

    private long expectedNanos(Fraction start) {
        return session.timing().tempo().nanosFor(start);
    }
}
