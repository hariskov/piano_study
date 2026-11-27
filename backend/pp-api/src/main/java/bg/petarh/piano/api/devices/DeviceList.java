package bg.petarh.piano.api.devices;

import java.util.List;

record DeviceList(List<DeviceInfo> inputs, List<DeviceInfo> outputs) {
}
