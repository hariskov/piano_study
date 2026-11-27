package bg.petarh.piano.app.echo;

import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.input.InputListener;
import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.midi.output.OutputEvent;
import bg.petarh.piano.midi.value.MidiChannel;

public final class InputEcho implements InputListener {

    private final OutputConnection output;
    private final MidiChannel channel;

    public InputEcho(OutputConnection output, MidiChannel channel) {
        this.output = output;
        this.channel = channel;
    }

    @Override
    public void onEvent(InputEvent event) {
        output.send(switch (event) {
            case InputEvent.NoteOn on -> new OutputEvent.NoteOn(on.pitch(), on.velocity(), channel);
            case InputEvent.NoteOff off -> new OutputEvent.NoteOff(off.pitch(), channel);
            case InputEvent.SustainPedal pedal -> new OutputEvent.SustainPedal(pedal.down(), channel);
        });
    }
}
