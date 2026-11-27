package bg.petarh.piano.engine.metronome;

import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.midi.output.OutputEvent;
import bg.petarh.piano.midi.value.MidiChannel;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;

public final class MidiClicks implements ClickOutput {

    private static final MidiChannel GENERAL_MIDI_PERCUSSION = MidiChannel.of(9);
    private static final Pitch HIGH_WOOD_BLOCK = Pitch.of(76);
    private static final Pitch LOW_WOOD_BLOCK = Pitch.of(77);
    private static final Velocity ACCENT = Velocity.of(110);
    private static final Velocity NORMAL = Velocity.of(80);

    private final OutputConnection output;

    public MidiClicks(OutputConnection output) {
        this.output = output;
    }

    @Override
    public void click(boolean accent) {
        Pitch sound = accent ? HIGH_WOOD_BLOCK : LOW_WOOD_BLOCK;
        output.send(new OutputEvent.NoteOn(sound, accent ? ACCENT : NORMAL, GENERAL_MIDI_PERCUSSION));
        output.send(new OutputEvent.NoteOff(sound, GENERAL_MIDI_PERCUSSION));
    }
}
