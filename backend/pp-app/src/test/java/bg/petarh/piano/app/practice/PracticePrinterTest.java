package bg.petarh.piano.app.practice;

import bg.petarh.piano.engine.evaluation.Evaluation.CorrectNote;
import bg.petarh.piano.engine.evaluation.Evaluation.MissedNote;
import bg.petarh.piano.engine.evaluation.Evaluation.TargetCompleted;
import bg.petarh.piano.engine.evaluation.Evaluation.TimedNote;
import bg.petarh.piano.engine.evaluation.Evaluation.WrongNote;
import bg.petarh.piano.engine.evaluation.TimingGrade;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PracticePrinterTest {

    private static final NoteEvent C4 = new NoteEvent(Pitch.parse("C4"), Fraction.ZERO, Fraction.of(1, 4), Hand.RIGHT);
    private static final long MS = 1_000_000L;

    private final StringWriter out = new StringWriter();
    private final PracticePrinter printer = new PracticePrinter(new PrintWriter(out, true), TargetNames::describe);

    @Test
    void printsEachTargetToPlay() {
        printer.onTargetChanged(Optional.of(Target.of(List.of(C4))));
        printer.onTargetChanged(Optional.empty());

        assertThat(out.toString().lines()).containsExactly("Play: C4");
    }

    @Test
    void printsTheResultOfEachPress() {
        printer.onEvaluation(new CorrectNote(C4.pitch(), 0));
        printer.onEvaluation(new WrongNote(Pitch.parse("D4"), 0));
        printer.onEvaluation(new TargetCompleted(Target.of(List.of(C4)), 0));
        printer.onEvaluation(new TimedNote(C4, TimingGrade.GOOD, -72 * MS, 0));
        printer.onEvaluation(new TimedNote(C4, TimingGrade.PERFECT, 12 * MS, 0));
        printer.onEvaluation(new MissedNote(C4, 0));

        assertThat(out.toString().lines()).containsExactly(
                "  ok       C4",
                "  wrong    D4",
                "  good     C4   72 ms early",
                "  perfect  C4   12 ms late",
                "  missed   C4");
    }

    @Test
    void printsStateChanges() {
        printer.onStateChanged(SessionState.COUNT_IN);
        printer.onStateChanged(SessionState.RUNNING);
        printer.onStateChanged(SessionState.PAUSED);
        printer.onStateChanged(SessionState.RUNNING);
        printer.onStateChanged(SessionState.FINISHED);

        assertThat(out.toString().lines()).containsExactly("Count-in...", "Go!", "Paused.", "Go!", "Finished.");
    }

    @Test
    void keepsATallyForTheSummary() {
        printer.onEvaluation(new WrongNote(Pitch.parse("D4"), 0));

        assertThat(printer.tally().summary(java.time.Duration.ZERO)).contains("1 wrong key");
    }
}
