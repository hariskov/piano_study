package bg.petarh.piano.api.session;

import bg.petarh.piano.engine.run.Summary;
import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.model.note.NoteEvent;

import java.util.List;

final class Messages {

    private Messages() {
    }

    record SessionStarted(long seed) {
    }

    record TempoParams(int bpm) {
    }

    record StateEvent(String state) {
    }

    record NoteInfo(String pitch, int midi, String hand) {

        static NoteInfo of(NoteEvent note) {
            return new NoteInfo(note.pitch().name(), note.pitch().midiNumber(), note.hand().name());
        }
    }

    record TargetEvent(String start, String name, List<NoteInfo> notes) {
    }

    record EvalEvent(String result, String pitch, String grade, Long offsetMs) {
    }

    record BeatEvent(boolean accent) {
    }

    record InputMessage(String type, String pitch, Integer midi, Integer velocity, Boolean down, long t) {

        static InputMessage of(InputEvent event) {
            return switch (event) {
                case InputEvent.NoteOn on -> new InputMessage("NOTE_ON", on.pitch().name(), on.pitch().midiNumber(),
                        on.velocity().value(), null, on.timestampNanos());
                case InputEvent.NoteOff off -> new InputMessage("NOTE_OFF", off.pitch().name(),
                        off.pitch().midiNumber(), null, null, off.timestampNanos());
                case InputEvent.SustainPedal pedal -> new InputMessage("PEDAL", null, null, null, pedal.down(),
                        pedal.timestampNanos());
            };
        }
    }

    record SummaryEvent(boolean timed, int completedTargets, int correctNotes, int wrongKeys, int perfect, int good,
                        int ok, int missed, int accuracyPercent, long elapsedMs) {

        static SummaryEvent of(Summary summary, long elapsedNanos) {
            return new SummaryEvent(summary.timed(), summary.completedTargets(), summary.correctNotes(),
                    summary.wrongKeys(), summary.perfect(), summary.good(), summary.ok(), summary.missed(),
                    summary.accuracyPercent(), elapsedNanos / 1_000_000L);
        }
    }
}
