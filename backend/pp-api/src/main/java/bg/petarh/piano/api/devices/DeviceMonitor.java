package bg.petarh.piano.api.devices;

import bg.petarh.piano.api.rpc.EventSink;
import bg.petarh.piano.api.rpc.RpcException;
import bg.petarh.piano.api.sound.KeySound;
import bg.petarh.piano.library.settings.Settings;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.output.InputEcho;
import bg.petarh.piano.midi.output.MidiOutputPort;
import bg.petarh.piano.midi.output.OutputConnection;

import java.util.Optional;
import java.util.function.Supplier;

public final class DeviceMonitor implements AutoCloseable {

    private final MidiInputPort inputs;
    private final MidiOutputPort outputs;
    private final EventSink events;
    private final Supplier<Settings> settings;
    private InputConnection monitored;
    private OutputConnection sounding;

    public DeviceMonitor(MidiInputPort inputs, MidiOutputPort outputs, EventSink events, Supplier<Settings> settings) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.events = events;
        this.settings = settings;
    }

    public synchronized void start(String input) {
        if (input == null) {
            throw new IllegalArgumentException("input is required: the MIDI input device id");
        }
        stop();
        try {
            Optional<InputEcho> echo = KeySound.echo(settings.get(), id -> sounding = outputs.open(DeviceId.of(id)));
            monitored = inputs.open(DeviceId.of(input), event -> {
                InputMessage.EVENT.send(events, InputMessage.of(event));
                echo.ifPresent(sound -> sound.onEvent(event));
            });
        } catch (DeviceUnavailableException e) {
            stop();
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
        if (sounding != null) {
            sounding.close();
            sounding = null;
        }
    }

    @Override
    public void close() {
        stop();
    }
}
