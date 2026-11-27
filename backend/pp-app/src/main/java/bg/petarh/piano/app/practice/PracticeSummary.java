package bg.petarh.piano.app.practice;

import bg.petarh.piano.engine.run.Summary;

import java.time.Duration;

public final class PracticeSummary {

    private PracticeSummary() {
    }

    public static String describe(Summary summary, Duration elapsed) {
        if (summary.timed()) {
            return "Done: perfect " + summary.perfect() + ", good " + summary.good() + ", ok " + summary.ok()
                    + ", missed " + summary.missed() + ", wrong keys " + summary.wrongKeys()
                    + ", accuracy " + summary.accuracyPercent() + "%, time " + minutes(elapsed) + ".";
        }
        return "Done: " + plural(summary.completedTargets(), "target") + ", " + plural(summary.wrongKeys(), "wrong key")
                + ", accuracy " + summary.accuracyPercent() + "%, time " + minutes(elapsed) + ".";
    }

    private static String plural(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    private static String minutes(Duration elapsed) {
        return elapsed.toMinutes() + ":" + String.format("%02d", elapsed.toSecondsPart());
    }
}
