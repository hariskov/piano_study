package bg.petarh.piano.api.devices;

import bg.petarh.piano.api.rpc.EventSink;
import bg.petarh.piano.api.rpc.RpcException;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.midi.input.MidiInputPort;

public final class DeviceMonitor implements AutoCloseable {

    private final MidiInputPort inputs;
    private final EventSink events;
    private InputConnection monitored;

    public DeviceMonitor(MidiInputPort inputs, EventSink events) {
        this.inputs = inputs;
        this.events = events;
    }

    public synchronized void start(String input) {
        if (input == null) {
            throw new IllegalArgumentException("input is required: the MIDI input device id");
        }
        stop();
        try {
            monitored = inputs.open(DeviceId.of(input), event -> InputMessage.EVENT.send(events, InputMessage.of(event)));
        } catch (DeviceUnavailableException e) {
            throw new RpcException(RpcException.DEVICE_ERROR, e.getMessage());
        }
    }

    public synchronized void stop() {
        if (monitored != null) {
            try {
                monitored.close();
            } catch (RuntimeException e) {
                // An unplugged device can fail to close; it is gone either way.
            }
            monitored = null;
        }
    }

    @Override
    public void close() {
        stop();
    }
}
