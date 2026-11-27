package bg.petarh.piano.api;

import bg.petarh.piano.api.devices.DeviceMethods;
import bg.petarh.piano.api.rpc.EventSink;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.session.SessionMethods;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.input.TimestampSource;
import bg.petarh.piano.midi.output.MidiOutputPort;

public final class PianoApi implements AutoCloseable {

    private final SessionMethods sessions;

    private PianoApi(SessionMethods sessions) {
        this.sessions = sessions;
    }

    public static PianoApi create(RpcServer server, MidiInputPort inputs, MidiOutputPort outputs,
                                  TimestampSource clock, EventSink events) {
        DeviceMethods.register(server, inputs, outputs);
        return new PianoApi(SessionMethods.register(server, inputs, outputs, clock, events));
    }

    @Override
    public void close() {
        sessions.close();
    }
}
