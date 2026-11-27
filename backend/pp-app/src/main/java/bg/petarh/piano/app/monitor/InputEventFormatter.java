package bg.petarh.piano.app.monitor;

import bg.petarh.piano.midi.input.InputEvent;

import java.util.Locale;

public final class InputEventFormatter {

    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final long startNanos;

    public InputEventFormatter(long startNanos) {
        this.startNanos = startNanos;
    }

    public String format(InputEvent event) {
        String elapsed = String.format(Locale.ROOT, "%9.3f s  ", (event.timestampNanos() - startNanos) / NANOS_PER_SECOND);
        return elapsed + switch (event) {
            case InputEvent.NoteOn on ->
                    String.format(Locale.ROOT, "NOTE ON   %-4s vel %3d", on.pitch(), on.velocity().value());
            case InputEvent.NoteOff off -> "NOTE OFF  " + off.pitch();
            case InputEvent.SustainPedal pedal -> "PEDAL     " + (pedal.down() ? "down" : "up");
        };
    }
}
