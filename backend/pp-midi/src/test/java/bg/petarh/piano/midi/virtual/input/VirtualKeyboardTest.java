package bg.petarh.piano.midi.virtual.input;

import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class VirtualKeyboardTest {

    private static final Pitch MIDDLE_C = Pitch.parse("C4");
    private static final Velocity MEDIUM = Velocity.of(90);

    private final AtomicLong now = new AtomicLong(1_000L);
    private final List<InputEvent> received = new ArrayList<>();
    private final VirtualKeyboard keyboard = VirtualKeyboard.withClock(now::get);
    private final MidiInputPort port = keyboard.port();

    @Test
    void portOffersOnlyTheVirtualKeyboardDevice() {
        assertThat(port.devices()).containsExactly(keyboard.device());
    }

    @Test
    void portRejectsUnknownDevice() {
        assertThatExceptionOfType(DeviceUnavailableException.class)
                .isThrownBy(() -> port.open(DeviceId.of("missing"), received::add));
    }

    @Test
    void deliversTimestampedEventsToOpenConnection() {
        port.open(keyboard.device().id(), received::add);

        keyboard.press(MIDDLE_C, MEDIUM);
        now.set(2_000L);
        keyboard.release(MIDDLE_C);
        now.set(3_000L);
        keyboard.sustain(true);

        assertThat(received).containsExactly(
                new InputEvent.NoteOn(MIDDLE_C, MEDIUM, 1_000L),
                new InputEvent.NoteOff(MIDDLE_C, 2_000L),
                new InputEvent.SustainPedal(true, 3_000L));
    }

    @Test
    void ignoresKeysPressedWhileNothingIsConnected() {
        keyboard.press(MIDDLE_C, MEDIUM);

        port.open(keyboard.device().id(), received::add);

        assertThat(received).isEmpty();
    }

    @Test
    void stopsDeliveringAfterConnectionIsClosed() {
        InputConnection connection = port.open(keyboard.device().id(), received::add);

        connection.close();
        keyboard.press(MIDDLE_C, MEDIUM);

        assertThat(received).isEmpty();
    }

    @Test
    void deliversToEveryOpenConnection() {
        List<InputEvent> second = new ArrayList<>();
        port.open(keyboard.device().id(), received::add);
        port.open(keyboard.device().id(), second::add);

        keyboard.press(MIDDLE_C, MEDIUM);

        assertThat(received).hasSize(1);
        assertThat(second).hasSize(1);
    }

    @Test
    void connectionReportsItsDevice() {
        InputConnection connection = port.open(keyboard.device().id(), received::add);

        assertThat(connection.device()).isEqualTo(keyboard.device());
    }
}
