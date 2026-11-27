package bg.petarh.piano.app.practice;

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

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PracticeTallyTest {

    private static final NoteEvent C4 = new NoteEvent(Pitch.parse("C4"), Fraction.ZERO, Fraction.of(1, 4), Hand.RIGHT);

    @Test
    void summarisesAWaitSession() {
        PracticeTally tally = new PracticeTally();
        tally.add(new CorrectNote(C4.pitch(), 0));
        tally.add(new TargetCompleted(Target.of(List.of(C4)), 0));
        tally.add(new WrongNote(Pitch.parse("D4"), 0));
        tally.add(new CorrectNote(C4.pitch(), 0));
        tally.add(new CorrectNote(C4.pitch(), 0));
        tally.add(new TargetCompleted(Target.of(List.of(C4)), 0));

        assertThat(tally.summary(Duration.ofSeconds(83)))
                .isEqualTo("Done: 2 targets, 1 wrong key, accuracy 75%, time 1:23.");
    }

    @Test
    void summarisesATempoSession() {
        PracticeTally tally = new PracticeTally();
        tally.add(new TimedNote(C4, TimingGrade.PERFECT, 0, 0));
        tally.add(new TimedNote(C4, TimingGrade.PERFECT, 0, 0));
        tally.add(new TimedNote(C4, TimingGrade.GOOD, 0, 0));
        tally.add(new MissedNote(C4, 0));
        tally.add(new WrongNote(Pitch.parse("D4"), 0));
        tally.add(new WrongNote(Pitch.parse("D4"), 0));

        assertThat(tally.summary(Duration.ofSeconds(5)))
                .isEqualTo("Done: perfect 2, good 1, ok 0, missed 1, wrong keys 2, accuracy 75%, time 0:05.");
    }

    @Test
    void summarisesASessionWithNothingPlayed() {
        assertThat(new PracticeTally().summary(Duration.ZERO))
                .isEqualTo("Done: 0 targets, 0 wrong keys, accuracy 0%, time 0:00.");
    }
}
