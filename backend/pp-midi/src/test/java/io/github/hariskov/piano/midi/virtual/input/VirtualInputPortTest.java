package io.github.hariskov.piano.midi.virtual.input;

import io.github.hariskov.piano.midi.device.DeviceId;
import io.github.hariskov.piano.midi.device.DeviceUnavailableException;
import io.github.hariskov.piano.midi.input.InputConnection;
import io.github.hariskov.piano.midi.input.InputEvent;
import io.github.hariskov.piano.midi.value.Velocity;
import io.github.hariskov.piano.model.pitch.Pitch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class VirtualInputPortTest {

    private static final Pitch MIDDLE_C = Pitch.parse("C4");
    private static final Velocity MEDIUM = Velocity.of(90);

    private final AtomicLong now = new AtomicLong(1_000L);
    private final List<InputEvent> received = new ArrayList<>();
    private VirtualInputPort port;

    @BeforeEach
    void setUp() {
        port = new VirtualInputPort(now::get);
    }

    @Test
    void offersSingleVirtualKeyboardDevice() {
        assertThat(port.devices()).singleElement()
                .satisfies(device -> assertThat(device.id()).isEqualTo(VirtualInputPort.DEVICE_ID));
    }

    @Test
    void rejectsUnknownDevice() {
        assertThatExceptionOfType(DeviceUnavailableException.class)
                .isThrownBy(() -> port.open(DeviceId.of("missing"), received::add));
    }

    @Test
    void deliversTimestampedEventsToOpenConnection() {
        port.open(VirtualInputPort.DEVICE_ID, received::add);

        port.press(MIDDLE_C, MEDIUM);
        now.set(2_000L);
        port.release(MIDDLE_C);
        now.set(3_000L);
        port.sustain(true);

        assertThat(received).containsExactly(
                new InputEvent.NoteOn(MIDDLE_C, MEDIUM, 1_000L),
                new InputEvent.NoteOff(MIDDLE_C, 2_000L),
                new InputEvent.SustainPedal(true, 3_000L));
    }

    @Test
    void ignoresKeysPressedWhileNothingIsConnected() {
        port.press(MIDDLE_C, MEDIUM);

        port.open(VirtualInputPort.DEVICE_ID, received::add);

        assertThat(received).isEmpty();
    }

    @Test
    void stopsDeliveringAfterConnectionIsClosed() {
        InputConnection connection = port.open(VirtualInputPort.DEVICE_ID, received::add);

        connection.close();
        port.press(MIDDLE_C, MEDIUM);

        assertThat(received).isEmpty();
    }

    @Test
    void deliversToEveryOpenConnection() {
        List<InputEvent> second = new ArrayList<>();
        port.open(VirtualInputPort.DEVICE_ID, received::add);
        port.open(VirtualInputPort.DEVICE_ID, second::add);

        port.press(MIDDLE_C, MEDIUM);

        assertThat(received).hasSize(1);
        assertThat(second).hasSize(1);
    }
}
