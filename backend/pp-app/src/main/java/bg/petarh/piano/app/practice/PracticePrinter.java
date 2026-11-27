package bg.petarh.piano.app.practice;

import bg.petarh.piano.engine.evaluation.Evaluation;
import bg.petarh.piano.engine.run.SessionListener;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;

import java.io.PrintWriter;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

public final class PracticePrinter implements SessionListener {

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final PrintWriter out;
    private final Function<Target, String> names;

    public PracticePrinter(PrintWriter out, Function<Target, String> names) {
        this.out = out;
        this.names = names;
    }

    @Override
    public void onStateChanged(SessionState state) {
        switch (state) {
            case READY -> {
            }
            case COUNT_IN -> out.println("Count-in...");
            case RUNNING -> out.println("Go!");
            case PAUSED -> out.println("Paused.");
            case FINISHED -> out.println("Finished.");
        }
    }

    @Override
    public void onTargetChanged(Optional<Target> target) {
        target.ifPresent(next -> out.println("Play: " + names.apply(next)));
    }

    @Override
    public void onEvaluation(Evaluation evaluation) {
        switch (evaluation) {
            case Evaluation.CorrectNote correct -> print("ok", correct.pitch());
            case Evaluation.WrongNote wrong -> print("wrong", wrong.pitch());
            case Evaluation.TargetCompleted ignored -> {
            }
            case Evaluation.TimedNote timed -> out.println(line(timed.grade().name().toLowerCase(Locale.ROOT),
                    String.format("%-5s", timed.note().pitch().name()) + offset(timed.offsetNanos())));
            case Evaluation.MissedNote missed -> print("missed", missed.note().pitch());
        }
    }

    private void print(String label, Pitch pitch) {
        out.println(line(label, pitch.name()));
    }

    private static String line(String label, String detail) {
        return "  " + String.format("%-9s", label) + detail;
    }

    private static String offset(long offsetNanos) {
        long millis = Math.round((double) offsetNanos / NANOS_PER_MILLI);
        if (millis == 0) {
            return "on time";
        }
        return Math.abs(millis) + " ms " + (millis < 0 ? "early" : "late");
    }
}
