package bg.petarh.piano.midi.jdk;

import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

class JdkMidiInputPortTest {

    private static final DeviceId PIANO_ID = DeviceId.of("jdk:Piano");

    private final FakeMidiDevice piano = FakeMidiDevice.input("Piano");
    private final AtomicLong now = new AtomicLong(5_000L);
    private final List<InputEvent> received = new ArrayList<>();
    private final JdkMidiInputPort port = new JdkMidiInputPort(new JdkDeviceCatalog(() -> List.of(piano)), now::get);

    @Test
    void listsInputDevices() {
        assertThat(port.devices()).singleElement()
                .satisfies(device -> assertThat(device.id()).isEqualTo(PIANO_ID));
    }

    @Test
    void rejectsUnknownDevice() {
        assertThatExceptionOfType(DeviceUnavailableException.class)
                .isThrownBy(() -> port.open(DeviceId.of("jdk:Missing"), received::add));
    }

    @Test
    void opensDeviceAndDeliversDecodedEventsWithArrivalTime() throws InvalidMidiDataException {
        port.open(PIANO_ID, received::add);

        piano.lastTransmitter().emit(new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 100));
        now.set(6_000L);
        piano.lastTransmitter().emit(new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 0));

        assertThat(piano.isOpen()).isTrue();
        assertThat(received).containsExactly(
                new InputEvent.NoteOn(Pitch.of(60), Velocity.of(100), 5_000L),
                new InputEvent.NoteOff(Pitch.of(60), 6_000L));
    }

    @Test
    void skipsMessagesWithoutMeaningForPractice() throws InvalidMidiDataException {
        port.open(PIANO_ID, received::add);

        piano.lastTransmitter().emit(new ShortMessage(ShortMessage.ACTIVE_SENSING));

        assertThat(received).isEmpty();
    }

    @Test
    void closingReleasesTransmitterAndDevice() throws InvalidMidiDataException {
        InputConnection connection = port.open(PIANO_ID, received::add);
        FakeTransmitter transmitter = piano.lastTransmitter();

        connection.close();
        transmitter.emit(new ShortMessage(ShortMessage.NOTE_ON, 0, 60, 100));

        assertThat(transmitter.isClosed()).isTrue();
        assertThat(piano.isOpen()).isFalse();
        assertThat(received).isEmpty();
    }

    @Test
    void reportsBusyDeviceAsUnavailable() {
        piano.failingOnOpen();

        assertThatExceptionOfType(DeviceUnavailableException.class)
                .isThrownBy(() -> port.open(PIANO_ID, received::add))
                .withCauseInstanceOf(MidiUnavailableException.class);
    }

    @Test
    void connectionReportsItsDevice() {
        InputConnection connection = port.open(PIANO_ID, received::add);

        assertThat(connection.device().id()).isEqualTo(PIANO_ID);
    }

    @Test
    void closesDeviceWhenTransmitterCannotBeObtained() {
        piano.failingOnTransmitter();

        assertThatExceptionOfType(DeviceUnavailableException.class)
                .isThrownBy(() -> port.open(PIANO_ID, received::add));
        assertThat(piano.isOpen()).isFalse();
    }

    @Test
    void closesDeviceEvenWhenTransmitterFailsToClose() {
        piano.withTransmittersFailingOnClose();
        InputConnection connection = port.open(PIANO_ID, received::add);

        assertThatIllegalStateException().isThrownBy(connection::close);
        assertThat(piano.isOpen()).isFalse();
    }
}
