package bg.petarh.piano.api.session;

import bg.petarh.piano.midi.input.InputEvent;

record LatencyOffset(long nanos) {

    private static final long NANOS_PER_MILLI = 1_000_000L;

    static LatencyOffset ofMillis(long millis) {
        return new LatencyOffset(millis * NANOS_PER_MILLI);
    }

    InputEvent apply(InputEvent event) {
        return switch (event) {
            case InputEvent.NoteOn on -> new InputEvent.NoteOn(on.pitch(), on.velocity(), on.timestampNanos() - nanos);
            case InputEvent.NoteOff off -> new InputEvent.NoteOff(off.pitch(), off.timestampNanos() - nanos);
            case InputEvent.SustainPedal pedal -> new InputEvent.SustainPedal(pedal.down(), pedal.timestampNanos() - nanos);
        };
    }
}
