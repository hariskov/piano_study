package bg.petarh.piano.midi.jdk.message;

import bg.petarh.piano.midi.output.OutputEvent;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

public final class OutputMessageEncoder {

    private static final int SUSTAIN_CONTROLLER = 64;
    private static final int ALL_NOTES_OFF_CONTROLLER = 123;
    private static final int CONTROLLER_ON = 127;
    private static final int CONTROLLER_OFF = 0;

    public ShortMessage encode(OutputEvent event) {
        return switch (event) {
            case OutputEvent.NoteOn on -> message(
                    ShortMessage.NOTE_ON, on.channel().index(), on.pitch().midiNumber(), on.velocity().value());
            case OutputEvent.NoteOff off -> message(
                    ShortMessage.NOTE_OFF, off.channel().index(), off.pitch().midiNumber(), 0);
            case OutputEvent.SustainPedal pedal -> message(
                    ShortMessage.CONTROL_CHANGE, pedal.channel().index(), SUSTAIN_CONTROLLER,
                    pedal.down() ? CONTROLLER_ON : CONTROLLER_OFF);
            case OutputEvent.ProgramChange change -> message(
                    ShortMessage.PROGRAM_CHANGE, change.channel().index(), change.instrument().program(), 0);
            case OutputEvent.AllNotesOff allOff -> message(
                    ShortMessage.CONTROL_CHANGE, allOff.channel().index(), ALL_NOTES_OFF_CONTROLLER, CONTROLLER_OFF);
        };
    }

    private static ShortMessage message(int command, int channel, int data1, int data2) {
        try {
            return new ShortMessage(command, channel, data1, data2);
        } catch (InvalidMidiDataException e) {
            // Unreachable: pitch, velocity and channel are already range-checked value objects.
            throw new IllegalStateException("Invalid MIDI data for validated event", e);
        }
    }
}
