package bg.petarh.piano.api.session;

import bg.petarh.piano.api.rpc.ApiEvent;
import bg.petarh.piano.api.rpc.MayBeAbsent;
import bg.petarh.piano.engine.run.Summary;
import bg.petarh.piano.model.note.NoteEvent;

import java.util.List;

public final class Messages {

    public static final ApiEvent<StateEvent> STATE = ApiEvent.of("session.state", StateEvent.class);
    public static final ApiEvent<TargetEvent> TARGET = ApiEvent.of("session.target", TargetEvent.class);
    public static final ApiEvent<EvalEvent> EVAL = ApiEvent.of("eval", EvalEvent.class);
    public static final ApiEvent<BeatEvent> BEAT = ApiEvent.of("beat", BeatEvent.class);
    public static final ApiEvent<SummaryEvent> SUMMARY = ApiEvent.of("session.summary", SummaryEvent.class);
    public static final ApiEvent<InputDeviceEvent> INPUT_LOST =
            ApiEvent.of("session.inputLost", InputDeviceEvent.class);
    public static final ApiEvent<InputDeviceEvent> INPUT_RESTORED =
            ApiEvent.of("session.inputRestored", InputDeviceEvent.class);

    private Messages() {
    }

    public record SessionStarted(long seed, int tempo, @MayBeAbsent Integer songTempo, List<String> notices) {
    }

    public record TempoParams(int bpm) {
    }

    public record StateEvent(String state) {
    }

    public record NoteInfo(String pitch, int midi, String hand) {

        static NoteInfo of(NoteEvent note) {
            return new NoteInfo(note.pitch().name(), note.pitch().midiNumber(), note.hand().name());
        }
    }

    public record TargetEvent(String start, @MayBeAbsent String name, List<NoteInfo> notes) {
    }

    public record EvalEvent(String result, @MayBeAbsent String pitch, @MayBeAbsent String grade,
                            @MayBeAbsent Long offsetMs) {
    }

    public record BeatEvent(boolean accent) {
    }

    public record InputDeviceEvent(String input) {
    }


    public record SummaryEvent(boolean timed, int completedTargets, int correctNotes, int wrongKeys, int perfect, int good,
                        int ok, int missed, int accuracyPercent, long elapsedMs) {

        static SummaryEvent of(Summary summary, long elapsedNanos) {
            return new SummaryEvent(summary.timed(), summary.completedTargets(), summary.correctNotes(),
                    summary.wrongKeys(), summary.perfect(), summary.good(), summary.ok(), summary.missed(),
                    summary.accuracyPercent(), elapsedNanos / 1_000_000L);
        }
    }
}
