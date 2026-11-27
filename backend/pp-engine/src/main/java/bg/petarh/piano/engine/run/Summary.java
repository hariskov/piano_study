package bg.petarh.piano.engine.run;

public record Summary(boolean timed, int completedTargets, int correctNotes, int wrongKeys,
                      int perfect, int good, int ok, int missed, int accuracyPercent) {
}
