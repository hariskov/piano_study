package bg.petarh.piano.api;

import bg.petarh.piano.api.devices.DeviceList;
import bg.petarh.piano.api.devices.DeviceMethods;
import bg.petarh.piano.api.devices.DeviceWatcher;
import bg.petarh.piano.api.rpc.ApiEvent;
import bg.petarh.piano.api.rpc.EventSink;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.session.Messages;
import bg.petarh.piano.api.session.SessionMethods;
import bg.petarh.piano.api.settings.SettingsMethods;
import bg.petarh.piano.api.settings.SettingsService;
import bg.petarh.piano.library.settings.SettingsStore;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.input.TimestampSource;
import bg.petarh.piano.midi.output.MidiOutputPort;

import java.time.Duration;
import java.util.List;

public final class PianoApi implements AutoCloseable {

    public static final List<ApiEvent<?>> EVENTS = List.of(Messages.STATE, Messages.TARGET, Messages.EVAL,
            Messages.INPUT, Messages.BEAT, Messages.SUMMARY, Messages.INPUT_LOST, Messages.INPUT_RESTORED,
            DeviceList.CHANGED);

    private final SessionMethods sessions;
    private final DeviceWatcher watcher;

    private PianoApi(SessionMethods sessions, DeviceWatcher watcher) {
        this.sessions = sessions;
        this.watcher = watcher;
    }

    public static PianoApi create(RpcServer server, MidiInputPort inputs, MidiOutputPort outputs,
                                  TimestampSource clock, EventSink events, SettingsStore store) {
        SettingsService settings = new SettingsService(store);
        DeviceMethods.register(server, inputs, outputs);
        SettingsMethods.register(server, settings);
        SessionMethods sessions = SessionMethods.register(server, inputs, outputs, clock, events, settings);
        return new PianoApi(sessions, new DeviceWatcher(inputs, outputs, events, sessions::inputsChanged));
    }

    public void startWatchingDevices(Duration interval) {
        watcher.start(interval);
    }

    @Override
    public void close() {
        watcher.close();
        sessions.close();
    }
}
