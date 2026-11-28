package bg.petarh.piano.api.session;

import bg.petarh.piano.api.devices.DeviceMonitor;
import bg.petarh.piano.api.devices.InputMessage;
import bg.petarh.piano.api.rpc.EventSink;
import bg.petarh.piano.api.rpc.RpcException;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.session.Messages.BeatEvent;
import bg.petarh.piano.api.session.Messages.InputDeviceEvent;
import bg.petarh.piano.api.session.Messages.SessionStarted;
import bg.petarh.piano.api.session.Messages.SummaryEvent;
import bg.petarh.piano.api.session.Messages.TempoParams;
import bg.petarh.piano.api.settings.SettingsService;
import bg.petarh.piano.engine.evaluation.FollowMode;
import bg.petarh.piano.engine.metronome.MidiClicks;
import bg.petarh.piano.engine.playback.MidiNotes;
import bg.petarh.piano.engine.playback.Playback;
import bg.petarh.piano.engine.run.Practice;
import bg.petarh.piano.engine.run.Results;
import bg.petarh.piano.engine.run.SessionListener;
import bg.petarh.piano.library.settings.Settings;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.midi.input.InputListener;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.input.TimestampSource;
import bg.petarh.piano.midi.output.MidiOutputPort;
import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.time.Tempo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class SessionMethods implements AutoCloseable {

    private static final class Active {

        private final Practice practice;
        private final String inputId;
        private final List<OutputConnection> outputs;
        private final String modeKey;
        private final PracticePlan plan;
        private final InputListener listener;
        private InputConnection input;
        private boolean inputLost;

        private Active(Practice practice, String inputId, List<OutputConnection> outputs, PracticePlan plan,
                       InputListener listener) {
            this.practice = practice;
            this.plan = plan;
            this.inputId = inputId;
            this.outputs = outputs;
            this.modeKey = plan.modeKey();
            this.listener = listener;
        }
    }



    private final MidiInputPort inputs;
    private final MidiOutputPort outputs;
    private final TimestampSource clock;
    private final EventSink events;
    private final SettingsService settings;
    private final DeviceMonitor monitor;
    private final Function<String, Optional<Score>> songs;
    private Active active;

    private SessionMethods(MidiInputPort inputs, MidiOutputPort outputs, TimestampSource clock, EventSink events,
                           SettingsService settings, DeviceMonitor monitor, Function<String, Optional<Score>> songs) {
        this.monitor = monitor;
        this.songs = songs;
        this.inputs = inputs;
        this.outputs = outputs;
        this.clock = clock;
        this.events = events;
        this.settings = settings;
    }

    public static SessionMethods register(RpcServer server, MidiInputPort inputs, MidiOutputPort outputs,
                                          TimestampSource clock, EventSink events, SettingsService settings,
                                          DeviceMonitor monitor, Function<String, Optional<Score>> songs) {
        SessionMethods methods = new SessionMethods(inputs, outputs, clock, events, settings, monitor, songs);
        server.register("session.start", SessionStartParams.class, SessionStarted.class, methods::start);
        server.register("session.pause", Void.class, Void.class, ignored -> methods.control(Practice::pause));
        server.register("session.resume", Void.class, Void.class, ignored -> methods.control(Practice::resume));
        server.register("session.changeTempo", TempoParams.class, Void.class, methods::changeTempo);
        server.register("session.stop", Void.class, Void.class, ignored -> methods.control(Practice::stop));
        server.register("session.adjust", AdjustParams.class, Void.class, methods::adjust);
        return methods;
    }

    @Override
    public void close() {
        current().ifPresent(Practice::stop);
    }

    private synchronized SessionStarted start(SessionStartParams params) {
        if (active != null) {
            throw new RpcException(RpcException.SESSION_ERROR, "A session is already running; stop it first");
        }
        Settings saved = settings.current();
        PracticePlan plan = "song".equals(params.exercise()) ? songPlan(params, saved) : PracticePlan.from(params, saved);
        monitor.stop();
        LatencyOffset latency = LatencyOffset.ofMillis(saved.latencyOffsetMs());
        Map<String, OutputConnection> outputs = openOutputs(params, plan);
        Optional<MidiClicks> clicks = Optional.ofNullable(params.metronome()).map(outputs::get).map(MidiClicks::new);
        Optional<Playback> playback = Optional.ofNullable(params.playback()).filter(id -> plan.isSong())
                .map(id -> new Playback(plan.playbackNotes(), new MidiNotes(outputs.get(id)), plan.mode() == FollowMode.WAIT));
        Results results = new Results();
        Practice practice = Practice.prepare(plan.targets(), plan.timing(), plan.mode(), clock::nanos,
                SessionListener.all(List.of(new SessionEvents(events, plan::nameOf), results)),
                Optional.of(accent -> {
                    Messages.BEAT.send(events, new BeatEvent(accent));
                    clicks.ifPresent(output -> output.click(accent));
                }), playback);
        InputListener listener = event -> {
            InputMessage.EVENT.send(events, InputMessage.of(event));
            practice.input().onEvent(latency.apply(event));
        };
        List<OutputConnection> opened = List.copyOf(outputs.values());
        Active session = new Active(practice, params.input(), opened, plan, listener);
        session.input = openInput(params.input(), listener, opened);
        active = session;
        int bpm = plan.timing().tempo().bpm();
        settings.update(current -> plan.isSong() ? current.withInput(params.input())
                : current.withInput(params.input()).withTempo(plan.modeKey(), bpm));
        practice.start().whenComplete((ignored, failure) -> finished(session, results));
        return new SessionStarted(plan.seed(), bpm, plan.songTempo(), plan.notices());
    }

    private Object adjust(AdjustParams change) {
        Active session = currentSession().orElseThrow(
                () -> new RpcException(RpcException.SESSION_ERROR, "No session is running"));
        Consumer<Optional<Target>> apply = session.plan.adjust(change);
        session.practice.onSessionThread(() -> apply.accept(session.practice.currentTarget()));
        return null;
    }

    private PracticePlan songPlan(SessionStartParams params, Settings saved) {
        if (params.songId() == null) {
            throw new IllegalArgumentException("songId is required for a song session");
        }
        Score song = songs.apply(params.songId())
                .orElseThrow(() -> new RpcException(RpcException.NOT_FOUND, "No song with id " + params.songId()));
        return PracticePlan.fromSong(params, saved, song);
    }

    private Object changeTempo(TempoParams params) {
        Tempo tempo = Tempo.of(params.bpm());
        Active session = currentSession().orElseThrow(
                () -> new RpcException(RpcException.SESSION_ERROR, "No session is running"));
        session.practice.changeTempo(tempo);
        if (!session.plan.isSong()) {
            settings.update(current -> current.withTempo(session.modeKey, tempo.bpm()));
        }
        return null;
    }

    private Object control(Consumer<Practice> action) {
        action.accept(current().orElseThrow(
                () -> new RpcException(RpcException.SESSION_ERROR, "No session is running")));
        return null;
    }

    public synchronized void inputsChanged(Set<String> available) {
        if (active == null) {
            return;
        }
        boolean present = available.contains(active.inputId);
        if (!active.inputLost && !present) {
            active.inputLost = true;
            active.practice.pause();
            closeQuietly(active.input);
            Messages.INPUT_LOST.send(events, new InputDeviceEvent(active.inputId));
        } else if (active.inputLost && present) {
            try {
                active.input = inputs.open(DeviceId.of(active.inputId), active.listener);
                active.inputLost = false;
                Messages.INPUT_RESTORED.send(events, new InputDeviceEvent(active.inputId));
            } catch (DeviceUnavailableException e) {
                // Listed but not openable yet; the next change in the device list tries again.
            }
        }
    }

    private Optional<Practice> current() {
        return currentSession().map(session -> session.practice);
    }

    private synchronized Optional<Active> currentSession() {
        return Optional.ofNullable(active);
    }

    private void finished(Active session, Results results) {
        synchronized (this) {
            if (!session.inputLost) {
                closeQuietly(session.input);
            }
            if (active == session) {
                active = null;
            }
        }
        session.outputs.forEach(OutputConnection::close);
        Messages.SUMMARY.send(events, SummaryEvent.of(results.summary(), session.practice.elapsedNanos()));
    }

    private static void closeQuietly(InputConnection input) {
        try {
            input.close();
        } catch (RuntimeException e) {
            // An unplugged device can fail to close; it is gone either way.
        }
    }

    private InputConnection openInput(String id, InputListener listener, List<OutputConnection> outputs) {
        try {
            return inputs.open(DeviceId.of(id), listener);
        } catch (DeviceUnavailableException e) {
            outputs.forEach(OutputConnection::close);
            throw new RpcException(RpcException.DEVICE_ERROR, e.getMessage());
        }
    }

    private Map<String, OutputConnection> openOutputs(SessionStartParams params, PracticePlan plan) {
        Map<String, OutputConnection> outputs = new LinkedHashMap<>();
        List<String> wanted = new ArrayList<>();
        Optional.ofNullable(params.metronome()).ifPresent(wanted::add);
        Optional.ofNullable(params.playback()).filter(id -> plan.isSong()).ifPresent(wanted::add);
        try {
            for (String id : wanted) {
                if (!outputs.containsKey(id)) {
                    outputs.put(id, openOutput(id));
                }
            }
        } catch (RuntimeException e) {
            outputs.values().forEach(OutputConnection::close);
            throw e;
        }
        return outputs;
    }

    private OutputConnection openOutput(String id) {
        try {
            return outputs.open(DeviceId.of(id));
        } catch (DeviceUnavailableException e) {
            throw new RpcException(RpcException.DEVICE_ERROR, e.getMessage());
        }
    }
}
