package bg.petarh.piano.midi.jdk;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JdkMidiSystemTest {

    @Test
    void listsRealInputDevicesWithoutSequencers() {
        assertThat(JdkMidi.inputPort().devices())
                .extracting(DeviceDescriptor::name)
                .doesNotContain("Real Time Sequencer");
    }

    @Test
    void listsBuiltInSoftwareSynthesizerAsOutput() {
        assertThat(JdkMidi.outputPort().devices())
                .extracting(DeviceDescriptor::name)
                .contains("Gervill");
    }
}
