package bg.petarh.piano.api.devices;

import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceScannerTest {

    private final SwitchableInputs inputs = new SwitchableInputs(VirtualKeyboard.withClock(() -> 0L).port());
    private final List<String> events = new CopyOnWriteArrayList<>();
    private final List<Set<String>> inputIds = new ArrayList<>();
    private final DeviceScanner scanner = new DeviceScanner(inputs, SilentOutput.create().port(),
            (method, params) -> events.add(method + " " + params), inputIds::add);

    @Test
    void staysQuietWhileNothingChanges() {
        scanner.rescan();
        scanner.rescan();

        assertThat(events).isEmpty();
        assertThat(inputIds).isEmpty();
    }

    @Test
    void reportsUnpluggedDevices() {
        inputs.unplug();

        scanner.rescan();

        assertThat(events).hasSize(1).first().asString().startsWith("devices.changed").contains("inputs=[]");
        assertThat(inputIds).containsExactly(Set.of());
    }

    @Test
    void reportsDevicesPluggedBackIn() {
        inputs.unplug();
        scanner.rescan();
        inputs.plugIn();

        scanner.rescan();

        assertThat(events).hasSize(2).last().asString().contains("virtual:keyboard");
        assertThat(inputIds).containsExactly(Set.of(), Set.of("virtual:keyboard"));
    }
}
