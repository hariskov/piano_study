package io.github.hariskov.piano.midi.jdk;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.device.DeviceId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JdkDeviceCatalogTest {

    private final FakeMidiDevice piano = FakeMidiDevice.input("Digital Piano [hw:2,0,0]");
    private final FakeMidiDevice speakers = FakeMidiDevice.output("Speakers");

    @Test
    void listsDevicesThatTransmitAsInputs() {
        JdkDeviceCatalog catalog = new JdkDeviceCatalog(() -> List.of(piano, speakers));

        assertThat(catalog.inputs()).extracting(DeviceDescriptor::name).containsExactly("Digital Piano [hw:2,0,0]");
    }

    @Test
    void listsDevicesThatReceiveAsOutputs() {
        JdkDeviceCatalog catalog = new JdkDeviceCatalog(() -> List.of(piano, speakers));

        assertThat(catalog.outputs()).extracting(DeviceDescriptor::name).containsExactly("Speakers");
    }

    @Test
    void derivesStableIdsFromDeviceNames() {
        JdkDeviceCatalog catalog = new JdkDeviceCatalog(() -> List.of(piano));

        assertThat(catalog.inputs()).extracting(DeviceDescriptor::id)
                .containsExactly(DeviceId.of("jdk:Digital Piano [hw:2,0,0]"));
    }

    @Test
    void numbersDevicesThatShareAName() {
        FakeMidiDevice first = FakeMidiDevice.input("USB MIDI");
        FakeMidiDevice second = FakeMidiDevice.input("USB MIDI");
        JdkDeviceCatalog catalog = new JdkDeviceCatalog(() -> List.of(first, second));

        assertThat(catalog.inputs()).extracting(DeviceDescriptor::id)
                .containsExactly(DeviceId.of("jdk:USB MIDI"), DeviceId.of("jdk:USB MIDI #2"));
    }

    @Test
    void findsInputDeviceById() {
        JdkDeviceCatalog catalog = new JdkDeviceCatalog(() -> List.of(piano, speakers));

        assertThat(catalog.findInput(DeviceId.of("jdk:Digital Piano [hw:2,0,0]")))
                .hasValueSatisfying(entry -> assertThat(entry.device()).isSameAs(piano));
        assertThat(catalog.findInput(DeviceId.of("jdk:Speakers"))).isEmpty();
    }

    @Test
    void rescansDevicesOnEveryCall() {
        List<FakeMidiDevice> connected = new ArrayList<>();
        JdkDeviceCatalog catalog = new JdkDeviceCatalog(() -> List.copyOf(connected));

        assertThat(catalog.inputs()).isEmpty();
        connected.add(piano);
        assertThat(catalog.inputs()).hasSize(1);
    }
}
