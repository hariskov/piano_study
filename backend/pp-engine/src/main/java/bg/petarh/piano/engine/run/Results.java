package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.Evaluation;
import bg.petarh.piano.engine.evaluation.TimingGrade;

import java.util.EnumMap;
import java.util.Map;

public final class Results implements SessionListener {

    private final Map<TimingGrade, Integer> grades = new EnumMap<>(TimingGrade.class);
    private int correctNotes;
    private int completedTargets;
    private int wrongKeys;
    private int misses;

    public Results() {
    }

    @Override
    public synchronized void onEvaluation(Evaluation evaluation) {
        switch (evaluation) {
            case Evaluation.CorrectNote ignored -> correctNotes++;
            case Evaluation.TargetCompleted ignored -> completedTargets++;
            case Evaluation.WrongNote ignored -> wrongKeys++;
            case Evaluation.TimedNote timed -> grades.merge(timed.grade(), 1, Integer::sum);
            case Evaluation.MissedNote ignored -> misses++;
        }
    }

    public synchronized Summary summary() {
        int timedNotes = grades.values().stream().mapToInt(Integer::intValue).sum();
        boolean timed = timedNotes + misses > 0;
        int accuracy = timed
                ? percent(timedNotes, timedNotes + misses)
                : percent(correctNotes, correctNotes + wrongKeys);
        return new Summary(timed, completedTargets, correctNotes, wrongKeys, grade(TimingGrade.PERFECT),
                grade(TimingGrade.GOOD), grade(TimingGrade.OK), misses, accuracy);
    }

    private int grade(TimingGrade grade) {
        return grades.getOrDefault(grade, 0);
    }

    private static int percent(int part, int whole) {
        return whole == 0 ? 0 : Math.round(100f * part / whole);
    }
}
