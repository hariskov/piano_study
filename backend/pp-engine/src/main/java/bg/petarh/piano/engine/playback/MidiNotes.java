package bg.petarh.piano.engine.playback;

import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.midi.output.OutputEvent;
import bg.petarh.piano.midi.value.MidiChannel;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;

public final class MidiNotes implements NoteOutput {

    private static final MidiChannel ACCOMPANIMENT = MidiChannel.of(1);
    private static final Velocity DEFAULT_VELOCITY = Velocity.of(72);

    private final OutputConnection output;
    private final Velocity velocity;

    public MidiNotes(OutputConnection output) {
        this(output, DEFAULT_VELOCITY);
    }

    public MidiNotes(OutputConnection output, Velocity velocity) {
        this.output = output;
        this.velocity = velocity;
    }

    @Override
    public void noteOn(Pitch pitch) {
        output.send(new OutputEvent.NoteOn(pitch, velocity, ACCOMPANIMENT));
    }

    @Override
    public void noteOff(Pitch pitch) {
        output.send(new OutputEvent.NoteOff(pitch, ACCOMPANIMENT));
    }

    @Override
    public void allOff() {
        output.send(new OutputEvent.AllNotesOff(ACCOMPANIMENT));
    }
}
