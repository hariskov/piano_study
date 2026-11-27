package io.github.hariskov.piano.midi.virtual.output;

import io.github.hariskov.piano.midi.device.DeviceId;
import io.github.hariskov.piano.midi.device.DeviceUnavailableException;
import io.github.hariskov.piano.midi.output.OutputConnection;
import io.github.hariskov.piano.midi.output.OutputEvent;
import io.github.hariskov.piano.midi.value.MidiChannel;
import io.github.hariskov.piano.midi.value.Velocity;
import io.github.hariskov.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class VirtualOutputPortTest {

    private static final Pitch MIDDLE_C = Pitch.parse("C4");
    private static final MidiChannel FIRST = MidiChannel.of(0);

    private final VirtualOutputPort port = new VirtualOutputPort();

    @Test
    void offersSingleSilentDevice() {
        assertThat(port.devices()).singleElement()
                .satisfies(device -> assertThat(device.id()).isEqualTo(VirtualOutputPort.DEVICE_ID));
    }

    @Test
    void rejectsUnknownDevice() {
        assertThatExceptionOfType(DeviceUnavailableException.class)
                .isThrownBy(() -> port.open(DeviceId.of("missing")));
    }

    @Test
    void recordsSentEventsInOrder() {
        OutputConnection connection = port.open(VirtualOutputPort.DEVICE_ID);

        connection.send(new OutputEvent.NoteOn(MIDDLE_C, Velocity.of(80), FIRST));
        connection.send(new OutputEvent.NoteOff(MIDDLE_C, FIRST));

        assertThat(port.sent()).containsExactly(
                new OutputEvent.NoteOn(MIDDLE_C, Velocity.of(80), FIRST),
                new OutputEvent.NoteOff(MIDDLE_C, FIRST));
    }

    @Test
    void rejectsSendingOnClosedConnection() {
        OutputConnection connection = port.open(VirtualOutputPort.DEVICE_ID);
        connection.close();

        assertThatIllegalStateException()
                .isThrownBy(() -> connection.send(new OutputEvent.AllNotesOff(FIRST)));
    }
}
