package bg.petarh.piano.app.device;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.midi.input.InputListener;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.output.MidiOutputPort;
import bg.petarh.piano.midi.output.OutputConnection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceListingTest {

    private static final DeviceDescriptor PIANO = new DeviceDescriptor(
            DeviceId.of("jdk:Digital Piano [hw:2,0,0]"), "Digital Piano [hw:2,0,0]", "USB MIDI keyboard");
    private static final DeviceDescriptor SYNTH = new DeviceDescriptor(
            DeviceId.of("jdk:Gervill"), "Gervill", "Software MIDI Synthesizer");
    private static final DeviceDescriptor PIANO_OUT = new DeviceDescriptor(
            DeviceId.of("jdk:Digital Piano [hw:2,0,0] out"), "Digital Piano [hw:2,0,0]", "USB MIDI keyboard");

    @Test
    void listsInputsAndOutputsWithTheIdsToUseInOtherCommands() {
        DeviceListing listing = new DeviceListing(inputs(PIANO), outputs(SYNTH, PIANO_OUT));

        assertThat(listing.lines()).containsExactly(
                "Inputs:",
                "  jdk:Digital Piano [hw:2,0,0]       USB MIDI keyboard",
                "Outputs:",
                "  jdk:Gervill                        Software MIDI Synthesizer",
                "  jdk:Digital Piano [hw:2,0,0] out   USB MIDI keyboard");
    }

    @Test
    void saysSoWhenNoDevicesAreFound() {
        DeviceListing listing = new DeviceListing(inputs(), outputs());

        assertThat(listing.lines()).containsExactly(
                "Inputs:",
                "  (none found - is the piano connected and switched on?)",
                "Outputs:",
                "  (none found)");
    }

    private static MidiInputPort inputs(DeviceDescriptor... devices) {
        return new MidiInputPort() {
            @Override
            public List<DeviceDescriptor> devices() {
                return List.of(devices);
            }

            @Override
            public InputConnection open(DeviceId id, InputListener listener) {
                throw new DeviceUnavailableException(id);
            }
        };
    }

    private static MidiOutputPort outputs(DeviceDescriptor... devices) {
        return new MidiOutputPort() {
            @Override
            public List<DeviceDescriptor> devices() {
                return List.of(devices);
            }

            @Override
            public OutputConnection open(DeviceId id) {
                throw new DeviceUnavailableException(id);
            }
        };
    }
}
