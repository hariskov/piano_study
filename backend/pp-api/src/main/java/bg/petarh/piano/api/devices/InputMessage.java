package bg.petarh.piano.api.devices;

import bg.petarh.piano.api.rpc.ApiEvent;
import bg.petarh.piano.api.rpc.MayBeAbsent;
import bg.petarh.piano.midi.input.InputEvent;

public record InputMessage(String type, @MayBeAbsent String pitch, @MayBeAbsent Integer midi,
                           @MayBeAbsent Integer velocity, @MayBeAbsent Boolean down, long t) {

    public static final ApiEvent<InputMessage> EVENT = ApiEvent.of("input", InputMessage.class);

    public static InputMessage of(InputEvent event) {
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
