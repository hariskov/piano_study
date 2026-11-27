package bg.petarh.piano.api.session;

import bg.petarh.piano.api.rpc.EventSink;
import bg.petarh.piano.api.session.Messages.EvalEvent;
import bg.petarh.piano.api.session.Messages.NoteInfo;
import bg.petarh.piano.api.session.Messages.StateEvent;
import bg.petarh.piano.api.session.Messages.TargetEvent;
import bg.petarh.piano.engine.evaluation.Evaluation;
import bg.petarh.piano.engine.run.SessionListener;
import bg.petarh.piano.engine.session.SessionState;
import bg.petarh.piano.model.note.Target;

import java.util.Optional;
import java.util.function.Function;

final class SessionEvents implements SessionListener {

    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final EventSink events;
    private final Function<Target, Optional<String>> names;

    SessionEvents(EventSink events, Function<Target, Optional<String>> names) {
        this.events = events;
        this.names = names;
    }

    @Override
    public void onStateChanged(SessionState state) {
        events.emit("session.state", new StateEvent(state.name()));
    }

    @Override
    public void onTargetChanged(Optional<Target> target) {
        target.ifPresent(next -> events.emit("session.target", new TargetEvent(next.start().toString(),
                names.apply(next).orElse(null), next.notes().stream().map(NoteInfo::of).toList())));
    }

    @Override
    public void onEvaluation(Evaluation evaluation) {
        events.emit("eval", switch (evaluation) {
            case Evaluation.CorrectNote correct -> new EvalEvent("CORRECT", correct.pitch().name(), null, null);
            case Evaluation.WrongNote wrong -> new EvalEvent("WRONG", wrong.pitch().name(), null, null);
            case Evaluation.TargetCompleted ignored -> new EvalEvent("TARGET_COMPLETED", null, null, null);
            case Evaluation.TimedNote timed -> new EvalEvent("TIMED", timed.note().pitch().name(),
                    timed.grade().name(), Math.round((double) timed.offsetNanos() / NANOS_PER_MILLI));
            case Evaluation.MissedNote missed -> new EvalEvent("MISSED", missed.note().pitch().name(), null, null);
        });
    }
}
