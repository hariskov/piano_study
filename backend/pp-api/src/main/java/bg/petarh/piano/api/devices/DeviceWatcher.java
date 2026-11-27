package bg.petarh.piano.api.devices;

import bg.petarh.piano.api.rpc.EventSink;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.output.MidiOutputPort;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class DeviceWatcher implements AutoCloseable {

    private final MidiInputPort inputs;
    private final MidiOutputPort outputs;
    private final EventSink events;
    private final Consumer<Set<String>> inputsChanged;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            task -> Thread.ofPlatform().name("device-watcher").daemon().unstarted(task));
    private DeviceList last;

    public DeviceWatcher(MidiInputPort inputs, MidiOutputPort outputs, EventSink events,
                         Consumer<Set<String>> inputsChanged) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.events = events;
        this.inputsChanged = inputsChanged;
        this.last = current();
    }

    public void start(Duration interval) {
        scheduler.scheduleWithFixedDelay(this::check, interval.toNanos(), interval.toNanos(), TimeUnit.NANOSECONDS);
    }

    synchronized void check() {
        DeviceList now = current();
        if (now.equals(last)) {
            return;
        }
        boolean inputsDiffer = !now.inputs().equals(last.inputs());
        last = now;
        events.emit("devices.changed", now);
        if (inputsDiffer) {
            inputsChanged.accept(now.inputs().stream().map(DeviceInfo::id).collect(Collectors.toSet()));
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }

    private DeviceList current() {
        return DeviceList.of(inputs, outputs);
    }
}
