package bg.petarh.piano.api.devices;

import bg.petarh.piano.api.rpc.ApiEvent;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.output.MidiOutputPort;

import java.util.List;

public record DeviceList(List<DeviceInfo> inputs, List<DeviceInfo> outputs) {

    public static final ApiEvent<DeviceList> CHANGED = ApiEvent.of("devices.changed", DeviceList.class);

    static DeviceList of(MidiInputPort inputs, MidiOutputPort outputs) {
        return new DeviceList(inputs.devices().stream().map(DeviceInfo::of).toList(),
                outputs.devices().stream().map(DeviceInfo::of).toList());
    }
}
