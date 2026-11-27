package io.github.hariskov.piano.midi.jdk;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
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
