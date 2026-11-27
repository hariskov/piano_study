package bg.petarh.piano.api;

import bg.petarh.piano.api.devices.DeviceMethods;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.output.MidiOutputPort;

public final class PianoApi {

    private PianoApi() {
    }

    public static void register(RpcServer server, MidiInputPort inputs, MidiOutputPort outputs) {
        DeviceMethods.register(server, inputs, outputs);
    }
}
