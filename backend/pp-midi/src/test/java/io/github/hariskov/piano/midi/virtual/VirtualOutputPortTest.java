package io.github.hariskov.piano.midi.virtual;

import io.github.hariskov.piano.midi.DeviceId;
import io.github.hariskov.piano.midi.DeviceUnavailableException;
import io.github.hariskov.piano.midi.OutputConnection;
import io.github.hariskov.piano.midi.OutputEvent;
import io.github.hariskov.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class VirtualOutputPortTest {

    private static final Pitch MIDDLE_C = Pitch.parse("C4");

    private final VirtualOutputPort port = new VirtualOutputPort();

    @Test
    void offersSingleSilentDevice() {
        assertThat(port.devices()).singleElement()
                .satisfies(device -> assertThat(device.id()).isEqualTo(VirtualOutputPort.DEVICE_ID));
    }

    @Test
    void rejectsUnknownDevice() {
        assertThatExceptionOfType(DeviceUnavailableException.class)
                .isThrownBy(() -> port.open(new DeviceId("missing")));
    }

    @Test
    void recordsSentEventsInOrder() {
        OutputConnection connection = port.open(VirtualOutputPort.DEVICE_ID);

        connection.send(new OutputEvent.NoteOn(MIDDLE_C, 80, 0));
        connection.send(new OutputEvent.NoteOff(MIDDLE_C, 0));

        assertThat(port.sent()).containsExactly(
                new OutputEvent.NoteOn(MIDDLE_C, 80, 0),
                new OutputEvent.NoteOff(MIDDLE_C, 0));
    }

    @Test
    void rejectsSendingOnClosedConnection() {
        OutputConnection connection = port.open(VirtualOutputPort.DEVICE_ID);
        connection.close();

        assertThatIllegalStateException()
                .isThrownBy(() -> connection.send(new OutputEvent.AllNotesOff(0)));
    }
}
