package bg.petarh.piano.midi.jdk.message;

import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import java.util.Optional;

public final class InputMessageDecoder {

    private static final int SUSTAIN_CONTROLLER = 64;
    private static final int SUSTAIN_DOWN_THRESHOLD = 64;

    public Optional<InputEvent> decode(MidiMessage message, long timestampNanos) {
        if (!(message instanceof ShortMessage shortMessage)) {
            return Optional.empty();
        }
        return switch (shortMessage.getCommand()) {
            case ShortMessage.NOTE_ON -> Optional.of(noteOn(shortMessage, timestampNanos));
            case ShortMessage.NOTE_OFF -> Optional.of(noteOff(shortMessage, timestampNanos));
            case ShortMessage.CONTROL_CHANGE -> controlChange(shortMessage, timestampNanos);
            default -> Optional.empty();
        };
    }

    private static InputEvent noteOn(ShortMessage message, long timestampNanos) {
        Pitch pitch = Pitch.of(message.getData1());
        int velocity = message.getData2();
        return velocity == 0
                ? new InputEvent.NoteOff(pitch, timestampNanos)
                : new InputEvent.NoteOn(pitch, Velocity.of(velocity), timestampNanos);
    }

    private static InputEvent noteOff(ShortMessage message, long timestampNanos) {
        return new InputEvent.NoteOff(Pitch.of(message.getData1()), timestampNanos);
    }

    private static Optional<InputEvent> controlChange(ShortMessage message, long timestampNanos) {
        if (message.getData1() != SUSTAIN_CONTROLLER) {
            return Optional.empty();
        }
        boolean down = message.getData2() >= SUSTAIN_DOWN_THRESHOLD;
        return Optional.of(new InputEvent.SustainPedal(down, timestampNanos));
    }
}
