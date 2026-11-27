package bg.petarh.piano.engine.evaluation;

import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;

public sealed interface Evaluation {

    long timestampNanos();

    record CorrectNote(Pitch pitch, long timestampNanos) implements Evaluation {
    }

    record WrongNote(Pitch pitch, long timestampNanos) implements Evaluation {
    }

    record TargetCompleted(Target target, long timestampNanos) implements Evaluation {
    }

    record TimedNote(NoteEvent note, TimingGrade grade, long offsetNanos, long timestampNanos) implements Evaluation {
    }

    record MissedNote(NoteEvent note, long timestampNanos) implements Evaluation {
    }
}
