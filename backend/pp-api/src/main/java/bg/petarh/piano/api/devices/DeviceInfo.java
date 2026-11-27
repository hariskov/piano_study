package bg.petarh.piano.api.devices;

import bg.petarh.piano.midi.device.DeviceDescriptor;

record DeviceInfo(String id, String name, String description) {

    static DeviceInfo of(DeviceDescriptor device) {
        return new DeviceInfo(device.id().value(), device.name(), device.description());
    }
}
