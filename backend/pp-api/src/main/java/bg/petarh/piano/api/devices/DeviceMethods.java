package bg.petarh.piano.api.devices;

import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.output.MidiOutputPort;

public final class DeviceMethods {

    private DeviceMethods() {
    }

    public static void register(RpcServer server, MidiInputPort inputs, MidiOutputPort outputs) {
        server.register("devices.list", Void.class, ignored -> DeviceList.of(inputs, outputs));
    }
}
