package bg.petarh.piano.api.devices;

import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.output.MidiOutputPort;

public final class DeviceMethods {

    private DeviceMethods() {
    }

    public record MonitorParams(String input) {
    }

    public static void register(RpcServer server, MidiInputPort inputs, MidiOutputPort outputs,
                                DeviceMonitor monitor) {
        server.register("devices.list", Void.class, DeviceList.class, ignored -> DeviceList.of(inputs, outputs));
        server.register("devices.monitor", MonitorParams.class, Void.class, params -> {
            monitor.start(params.input());
            return null;
        });
        server.register("devices.stopMonitor", Void.class, Void.class, ignored -> {
            monitor.stop();
            return null;
        });
    }
}
