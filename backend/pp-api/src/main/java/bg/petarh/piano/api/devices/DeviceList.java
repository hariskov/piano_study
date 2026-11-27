package bg.petarh.piano.api.devices;

import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.output.MidiOutputPort;

import java.util.List;

record DeviceList(List<DeviceInfo> inputs, List<DeviceInfo> outputs) {

    static DeviceList of(MidiInputPort inputs, MidiOutputPort outputs) {
        return new DeviceList(inputs.devices().stream().map(DeviceInfo::of).toList(),
                outputs.devices().stream().map(DeviceInfo::of).toList());
    }
}
