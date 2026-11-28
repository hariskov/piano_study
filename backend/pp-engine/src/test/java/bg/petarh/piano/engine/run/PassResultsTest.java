package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.Evaluation;
import bg.petarh.piano.engine.evaluation.TimingGrade;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PassResultsTest {

    private final PassResults results = new PassResults(position -> (int) position.floor() + 1);

    @Test
    void countsKeyPressesForThePassOfTheCurrentTarget() {
        results.onTargetChanged(Optional.of(target(Fraction.ZERO)));
        results.onEvaluation(new Evaluation.CorrectNote(Pitch.parse("C4"), 0));
        results.onEvaluation(new Evaluation.TargetCompleted(target(Fraction.ZERO), 0));
        results.onTargetChanged(Optional.of(target(Fraction.whole(1))));
        results.onEvaluation(new Evaluation.WrongNote(Pitch.parse("D4"), 0));
        results.onEvaluation(new Evaluation.CorrectNote(Pitch.parse("C4"), 0));

        assertThat(results.passes()).extracting(Summary::accuracyPercent).containsExactly(100, 50);
    }

    @Test
    void placesTimedNotesByWhereTheyAreInTheSong() {
        results.onTargetChanged(Optional.of(target(Fraction.whole(1))));
        results.onEvaluation(new Evaluation.MissedNote(note(Fraction.ZERO), 0));
        results.onEvaluation(new Evaluation.TimedNote(note(Fraction.whole(1)), TimingGrade.PERFECT, 0, 0));

        assertThat(results.passes()).extracting(Summary::accuracyPercent).containsExactly(0, 100);
    }

    @Test
    void hasNoPassesBeforeAnythingHappens() {
        assertThat(results.passes()).isEmpty();
    }

    private static Target target(Fraction start) {
        return Target.of(List.of(note(start)));
    }

    private static NoteEvent note(Fraction start) {
        return new NoteEvent(Pitch.parse("C4"), start, Fraction.of(1, 4), Hand.RIGHT);
    }
}
