package bg.petarh.piano.api.devices;

import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(5)
class DeviceWatcherTest {

    private final SwitchableInputs inputs = new SwitchableInputs(VirtualKeyboard.withClock(() -> 0L).port());
    private final List<String> events = new CopyOnWriteArrayList<>();
    private final List<Set<String>> inputIds = new ArrayList<>();
    private final DeviceWatcher watcher = new DeviceWatcher(inputs, SilentOutput.create().port(),
            (method, params) -> events.add(method + " " + params), inputIds::add);

    @Test
    void staysQuietWhileNothingChanges() {
        watcher.check();
        watcher.check();

        assertThat(events).isEmpty();
        assertThat(inputIds).isEmpty();
    }

    @Test
    void reportsUnpluggedDevices() {
        inputs.unplug();

        watcher.check();

        assertThat(events).hasSize(1).first().asString().startsWith("devices.changed").contains("inputs=[]");
        assertThat(inputIds).containsExactly(Set.of());
    }

    @Test
    void reportsDevicesPluggedBackIn() {
        inputs.unplug();
        watcher.check();
        inputs.plugIn();

        watcher.check();

        assertThat(events).hasSize(2).last().asString().contains("virtual:keyboard");
        assertThat(inputIds).containsExactly(Set.of(), Set.of("virtual:keyboard"));
    }

    @Test
    void checksOnItsOwnOnceStarted() {
        try (watcher) {
            watcher.start(Duration.ofMillis(5));
            inputs.unplug();
            while (events.isEmpty()) {
                Thread.onSpinWait();
            }
        }
        assertThat(events).first().asString().startsWith("devices.changed");
    }
}
