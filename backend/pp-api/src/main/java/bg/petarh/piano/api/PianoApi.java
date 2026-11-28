package bg.petarh.piano.api;

import bg.petarh.piano.api.devices.DeviceList;
import bg.petarh.piano.api.devices.DeviceMethods;
import bg.petarh.piano.api.devices.DeviceMonitor;
import bg.petarh.piano.api.devices.InputMessage;
import bg.petarh.piano.api.keyboard.KeyboardMethods;
import bg.petarh.piano.api.library.LibraryMethods;
import bg.petarh.piano.api.devices.DeviceWatcher;
import bg.petarh.piano.api.rpc.ApiEvent;
import bg.petarh.piano.api.rpc.EventSink;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.session.Messages;
import bg.petarh.piano.api.session.SessionMethods;
import bg.petarh.piano.api.settings.SettingsMethods;
import bg.petarh.piano.api.settings.SettingsService;
import bg.petarh.piano.library.settings.SettingsStore;
import bg.petarh.piano.library.songs.SongLibrary;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.input.TimestampSource;
import bg.petarh.piano.midi.output.MidiOutputPort;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;

import java.time.Duration;
import java.util.List;

public final class PianoApi implements AutoCloseable {

    public static final List<ApiEvent<?>> EVENTS = List.of(Messages.STATE, Messages.TARGET, Messages.EVAL,
            InputMessage.EVENT, Messages.BEAT, Messages.SUMMARY, Messages.INPUT_LOST, Messages.INPUT_RESTORED,
            DeviceList.CHANGED);

    private final SessionMethods sessions;
    private final DeviceWatcher watcher;
    private final DeviceMonitor monitor;

    private PianoApi(SessionMethods sessions, DeviceWatcher watcher, DeviceMonitor monitor) {
        this.sessions = sessions;
        this.watcher = watcher;
        this.monitor = monitor;
    }

    public static PianoApi create(RpcServer server, MidiInputPort inputs, MidiOutputPort outputs,
                                  TimestampSource clock, EventSink events, SettingsStore store) {
        return create(server, inputs, outputs, clock, events, store, VirtualKeyboard.withClock(clock));
    }

    public static PianoApi create(RpcServer server, MidiInputPort inputs, MidiOutputPort outputs,
                                  TimestampSource clock, EventSink events, SettingsStore store,
                                  VirtualKeyboard keyboard, SongLibrary library) {
        LibraryMethods.register(server, library);
        return create(server, inputs, outputs, clock, events, store, keyboard);
    }

    public static PianoApi create(RpcServer server, MidiInputPort inputs, MidiOutputPort outputs,
                                  TimestampSource clock, EventSink events, SettingsStore store,
                                  VirtualKeyboard keyboard) {
        SettingsService settings = new SettingsService(store);
        DeviceMonitor monitor = new DeviceMonitor(inputs, events);
        DeviceMethods.register(server, inputs, outputs, monitor);
        KeyboardMethods.register(server, keyboard);
        SettingsMethods.register(server, settings);
        SessionMethods sessions = SessionMethods.register(server, inputs, outputs, clock, events, settings, monitor);
        return new PianoApi(sessions, new DeviceWatcher(inputs, outputs, events, sessions::inputsChanged), monitor);
    }

    public void startWatchingDevices(Duration interval) {
        watcher.start(interval);
    }

    @Override
    public void close() {
        watcher.close();
        monitor.close();
        sessions.close();
    }
}
