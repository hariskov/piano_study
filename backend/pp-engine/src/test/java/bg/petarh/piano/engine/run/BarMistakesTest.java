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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BarMistakesTest {

    private final BarMistakes mistakes = new BarMistakes(position -> (int) position.floor() + 1);

    @Test
    void countsWrongKeysInTheBarOfTheCurrentTargetAndMissesWhereTheNoteWas() {
        mistakes.onTargetChanged(Optional.of(target(Fraction.of(5, 4))));
        mistakes.onEvaluation(new Evaluation.WrongNote(Pitch.parse("D4"), 0));
        mistakes.onEvaluation(new Evaluation.WrongNote(Pitch.parse("E4"), 0));
        mistakes.onEvaluation(new Evaluation.MissedNote(note(Fraction.of(1, 2)), 0));
        mistakes.onEvaluation(new Evaluation.TimedNote(note(Fraction.whole(3)), TimingGrade.GOOD, 0, 0));

        assertThat(mistakes.byBar()).isEqualTo(Map.of(2, 2, 1, 1));
    }

    @Test
    void ignoresWrongKeysBeforeTheFirstTarget() {
        mistakes.onEvaluation(new Evaluation.WrongNote(Pitch.parse("D4"), 0));

        assertThat(mistakes.byBar()).isEmpty();
    }

    private static Target target(Fraction start) {
        return Target.of(List.of(note(start)));
    }

    private static NoteEvent note(Fraction start) {
        return new NoteEvent(Pitch.parse("C4"), start, Fraction.of(1, 4), Hand.RIGHT);
    }
}
