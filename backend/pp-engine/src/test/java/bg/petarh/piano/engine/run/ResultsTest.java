package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.Evaluation.CorrectNote;
import bg.petarh.piano.engine.evaluation.Evaluation.MissedNote;
import bg.petarh.piano.engine.evaluation.Evaluation.TargetCompleted;
import bg.petarh.piano.engine.evaluation.Evaluation.TimedNote;
import bg.petarh.piano.engine.evaluation.Evaluation.WrongNote;
import bg.petarh.piano.engine.evaluation.TimingGrade;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResultsTest {

    private static final NoteEvent C4 = new NoteEvent(Pitch.parse("C4"), Fraction.ZERO, Fraction.of(1, 4), Hand.RIGHT);

    private final Results results = new Results();

    @Test
    void countsAWaitSession() {
        results.onEvaluation(new CorrectNote(C4.pitch(), 0));
        results.onEvaluation(new TargetCompleted(Target.of(List.of(C4)), 0));
        results.onEvaluation(new WrongNote(Pitch.parse("D4"), 0));
        results.onEvaluation(new CorrectNote(C4.pitch(), 0));
        results.onEvaluation(new CorrectNote(C4.pitch(), 0));
        results.onEvaluation(new TargetCompleted(Target.of(List.of(C4)), 0));

        assertThat(results.summary()).isEqualTo(new Summary(false, 2, 3, 1, 0, 0, 0, 0, 75));
    }

    @Test
    void countsATempoSession() {
        results.onEvaluation(new TimedNote(C4, TimingGrade.PERFECT, 0, 0));
        results.onEvaluation(new TimedNote(C4, TimingGrade.PERFECT, 0, 0));
        results.onEvaluation(new TimedNote(C4, TimingGrade.GOOD, 0, 0));
        results.onEvaluation(new MissedNote(C4, 0));
        results.onEvaluation(new WrongNote(Pitch.parse("D4"), 0));
        results.onEvaluation(new WrongNote(Pitch.parse("D4"), 0));

        assertThat(results.summary()).isEqualTo(new Summary(true, 0, 0, 2, 2, 1, 0, 1, 75));
    }

    @Test
    void hasNoAccuracyBeforeAnythingIsPlayed() {
        assertThat(results.summary()).isEqualTo(new Summary(false, 0, 0, 0, 0, 0, 0, 0, 0));
    }
}
