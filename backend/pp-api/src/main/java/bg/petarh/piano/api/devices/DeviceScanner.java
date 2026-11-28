package bg.petarh.piano.api.devices;

import bg.petarh.piano.api.rpc.EventSink;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.output.MidiOutputPort;

import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class DeviceScanner {

    private final MidiInputPort inputs;
    private final MidiOutputPort outputs;
    private final EventSink events;
    private final Consumer<Set<String>> inputsChanged;
    private DeviceList last;

    public DeviceScanner(MidiInputPort inputs, MidiOutputPort outputs, EventSink events,
                         Consumer<Set<String>> inputsChanged) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.events = events;
        this.inputsChanged = inputsChanged;
        this.last = DeviceList.of(inputs, outputs);
    }

    public synchronized DeviceList rescan() {
        DeviceList now = DeviceList.of(inputs, outputs);
        if (now.equals(last)) {
            return now;
        }
        boolean inputsDiffer = !now.inputs().equals(last.inputs());
        last = now;
        DeviceList.CHANGED.send(events, now);
        if (inputsDiffer) {
            inputsChanged.accept(now.inputs().stream().map(DeviceInfo::id).collect(Collectors.toSet()));
        }
        return now;
    }
}
