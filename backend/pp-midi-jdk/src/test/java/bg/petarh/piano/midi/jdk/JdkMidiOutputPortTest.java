package bg.petarh.piano.midi.jdk;

import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.midi.output.OutputEvent;
import bg.petarh.piano.midi.value.MidiChannel;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.tuple;

class JdkMidiOutputPortTest {

    private static final DeviceId SYNTH_ID = DeviceId.of("jdk:Synth");
    private static final MidiChannel FIRST = MidiChannel.of(0);
    private static final int ALL_NOTES_OFF = 123;

    private final FakeMidiDevice synth = FakeMidiDevice.output("Synth");
    private final JdkMidiOutputPort port = new JdkMidiOutputPort(new JdkDeviceCatalog(() -> List.of(synth)));

    @Test
    void playsASynthesizerWithTheSoundFontInsteadOfItsBuiltInSounds() {
        FakeSynthesizer gervill = new FakeSynthesizer();
        Soundbank fluid = FakeSynthesizer.soundbank("FluidR3");
        JdkMidiOutputPort withSoundFont = new JdkMidiOutputPort(new JdkDeviceCatalog(() -> List.of(gervill)),
                () -> Optional.of(fluid));

        withSoundFont.open(DeviceId.of("jdk:Gervill"));

        assertThat(gervill.loaded()).containsExactly(fluid);
    }

    @Test
    void keepsTheBuiltInSoundsWithoutASoundFont() {
        FakeSynthesizer gervill = new FakeSynthesizer();
        JdkMidiOutputPort without = new JdkMidiOutputPort(new JdkDeviceCatalog(() -> List.of(gervill)), Optional::empty);

        without.open(DeviceId.of("jdk:Gervill"));

        assertThat(gervill.loaded()).containsExactly(FakeSynthesizer.BUILT_IN);
    }

    @Test
    void listsOutputDevices() {
        assertThat(port.devices()).singleElement()
                .satisfies(device -> assertThat(device.id()).isEqualTo(SYNTH_ID));
    }

    @Test
    void rejectsUnknownDevice() {
        assertThatExceptionOfType(DeviceUnavailableException.class)
                .isThrownBy(() -> port.open(DeviceId.of("jdk:Missing")));
    }

    @Test
    void sendsEncodedEventsWithTheirVelocity() {
        OutputConnection connection = port.open(SYNTH_ID);

        connection.send(new OutputEvent.NoteOn(Pitch.of(60), Velocity.of(30), FIRST));
        connection.send(new OutputEvent.NoteOn(Pitch.of(64), Velocity.of(120), FIRST));

        assertThat(synth.isOpen()).isTrue();
        assertThat(synth.lastReceiver().received())
                .extracting(ShortMessage::getCommand, ShortMessage::getData1, ShortMessage::getData2)
                .containsExactly(
                        tuple(ShortMessage.NOTE_ON, 60, 30),
                        tuple(ShortMessage.NOTE_ON, 64, 120));
    }

    @Test
    void silencesEveryChannelBeforeClosing() {
        OutputConnection connection = port.open(SYNTH_ID);
        FakeReceiver receiver = synth.lastReceiver();

        connection.close();

        assertThat(receiver.received())
                .allSatisfy(message -> {
                    assertThat(message.getCommand()).isEqualTo(ShortMessage.CONTROL_CHANGE);
                    assertThat(message.getData1()).isEqualTo(ALL_NOTES_OFF);
                })
                .extracting(ShortMessage::getChannel)
                .containsExactlyElementsOf(IntStream.range(0, 16).boxed().toList());
        assertThat(receiver.isClosed()).isTrue();
        assertThat(synth.isOpen()).isFalse();
    }

    @Test
    void rejectsSendingAfterClose() {
        OutputConnection connection = port.open(SYNTH_ID);
        connection.close();

        assertThatIllegalStateException()
                .isThrownBy(() -> connection.send(new OutputEvent.AllNotesOff(FIRST)));
    }

    @Test
    void closingTwiceIsHarmless() {
        OutputConnection connection = port.open(SYNTH_ID);
        FakeReceiver receiver = synth.lastReceiver();

        connection.close();
        connection.close();

        assertThat(receiver.received()).hasSize(16);
    }

    @Test
    void reportsBusyDeviceAsUnavailable() {
        synth.failingOnOpen();

        assertThatExceptionOfType(DeviceUnavailableException.class)
                .isThrownBy(() -> port.open(SYNTH_ID))
                .withCauseInstanceOf(MidiUnavailableException.class);
    }

    @Test
    void closesDeviceWhenReceiverCannotBeObtained() {
        synth.failingOnReceiver();

        assertThatExceptionOfType(DeviceUnavailableException.class).isThrownBy(() -> port.open(SYNTH_ID));
        assertThat(synth.isOpen()).isFalse();
    }

    @Test
    void closesDeviceEvenWhenReceiverFailsToClose() {
        synth.withReceiversFailingOnClose();
        OutputConnection connection = port.open(SYNTH_ID);

        assertThatIllegalStateException().isThrownBy(connection::close);
        assertThat(synth.isOpen()).isFalse();
    }

    @Test
    void connectionReportsItsDevice() {
        assertThat(port.open(SYNTH_ID).device().id()).isEqualTo(SYNTH_ID);
    }
}
