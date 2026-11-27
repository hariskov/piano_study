package bg.petarh.piano.app.practice;

import bg.petarh.piano.engine.evaluation.Evaluation;
import bg.petarh.piano.engine.evaluation.TimingGrade;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

public final class PracticeTally {

    private final Map<TimingGrade, Integer> grades = new EnumMap<>(TimingGrade.class);
    private int correctNotes;
    private int completedTargets;
    private int wrongKeys;
    private int misses;

    public PracticeTally() {
    }

    public synchronized void add(Evaluation evaluation) {
        switch (evaluation) {
            case Evaluation.CorrectNote ignored -> correctNotes++;
            case Evaluation.TargetCompleted ignored -> completedTargets++;
            case Evaluation.WrongNote ignored -> wrongKeys++;
            case Evaluation.TimedNote timed -> grades.merge(timed.grade(), 1, Integer::sum);
            case Evaluation.MissedNote ignored -> misses++;
        }
    }

    public synchronized String summary(Duration elapsed) {
        int timed = grades.values().stream().mapToInt(Integer::intValue).sum();
        if (timed + misses > 0) {
            return "Done: perfect " + grade(TimingGrade.PERFECT) + ", good " + grade(TimingGrade.GOOD)
                    + ", ok " + grade(TimingGrade.OK) + ", missed " + misses + ", wrong keys " + wrongKeys
                    + ", accuracy " + percent(timed, timed + misses) + ", time " + minutes(elapsed) + ".";
        }
        return "Done: " + plural(completedTargets, "target") + ", " + plural(wrongKeys, "wrong key")
                + ", accuracy " + percent(correctNotes, correctNotes + wrongKeys) + ", time " + minutes(elapsed) + ".";
    }

    private int grade(TimingGrade grade) {
        return grades.getOrDefault(grade, 0);
    }

    private static String percent(int part, int whole) {
        return (whole == 0 ? 0 : Math.round(100f * part / whole)) + "%";
    }

    private static String plural(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    private static String minutes(Duration elapsed) {
        return elapsed.toMinutes() + ":" + String.format("%02d", elapsed.toSecondsPart());
    }
}
