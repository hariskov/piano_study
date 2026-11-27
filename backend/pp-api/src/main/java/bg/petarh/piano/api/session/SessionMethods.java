package bg.petarh.piano.api.session;

import bg.petarh.piano.api.rpc.EventSink;
import bg.petarh.piano.api.rpc.RpcException;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.session.Messages.BeatEvent;
import bg.petarh.piano.api.session.Messages.InputMessage;
import bg.petarh.piano.api.session.Messages.SessionStarted;
import bg.petarh.piano.api.session.Messages.SummaryEvent;
import bg.petarh.piano.api.session.Messages.TempoParams;
import bg.petarh.piano.api.settings.SettingsService;
import bg.petarh.piano.engine.metronome.MidiClicks;
import bg.petarh.piano.engine.run.Practice;
import bg.petarh.piano.engine.run.Results;
import bg.petarh.piano.engine.run.SessionListener;
import bg.petarh.piano.library.settings.Settings;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.input.TimestampSource;
import bg.petarh.piano.midi.output.MidiOutputPort;
import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.model.time.Tempo;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class SessionMethods implements AutoCloseable {

    private record Active(Practice practice, InputConnection input, Optional<OutputConnection> metronome,
                          String modeKey) {
    }

    private final MidiInputPort inputs;
    private final MidiOutputPort outputs;
    private final TimestampSource clock;
    private final EventSink events;
    private final SettingsService settings;
    private Active active;

    private SessionMethods(MidiInputPort inputs, MidiOutputPort outputs, TimestampSource clock, EventSink events,
                           SettingsService settings) {
        this.inputs = inputs;
        this.outputs = outputs;
        this.clock = clock;
        this.events = events;
        this.settings = settings;
    }

    public static SessionMethods register(RpcServer server, MidiInputPort inputs, MidiOutputPort outputs,
                                          TimestampSource clock, EventSink events, SettingsService settings) {
        SessionMethods methods = new SessionMethods(inputs, outputs, clock, events, settings);
        server.register("session.start", SessionStartParams.class, methods::start);
        server.register("session.pause", Void.class, ignored -> methods.control(Practice::pause));
        server.register("session.resume", Void.class, ignored -> methods.control(Practice::resume));
        server.register("session.changeTempo", TempoParams.class, methods::changeTempo);
        server.register("session.stop", Void.class, ignored -> methods.control(Practice::stop));
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
        PracticePlan plan = PracticePlan.from(params, saved);
        LatencyOffset latency = LatencyOffset.ofMillis(saved.latencyOffsetMs());
        Optional<OutputConnection> metronome = Optional.ofNullable(params.metronome()).map(this::openOutput);
        Optional<MidiClicks> clicks = metronome.map(MidiClicks::new);
        Results results = new Results();
        Practice practice = Practice.prepare(plan.targets(), plan.timing(), plan.mode(), clock::nanos,
                SessionListener.all(List.of(new SessionEvents(events, plan.names()), results)),
                Optional.of(accent -> {
                    events.emit("beat", new BeatEvent(accent));
                    clicks.ifPresent(output -> output.click(accent));
                }));
        InputConnection input = openInput(params.input(), practice, metronome, latency);
        Active session = new Active(practice, input, metronome, plan.modeKey());
        active = session;
        int bpm = plan.timing().tempo().bpm();
        settings.update(current -> current.withInput(params.input()).withTempo(plan.modeKey(), bpm));
        practice.start().whenComplete((ignored, failure) -> finished(session, results));
        return new SessionStarted(plan.seed(), bpm);
    }

    private Object changeTempo(TempoParams params) {
        Tempo tempo = Tempo.of(params.bpm());
        Active session = currentSession().orElseThrow(
                () -> new RpcException(RpcException.SESSION_ERROR, "No session is running"));
        session.practice().changeTempo(tempo);
        settings.update(current -> current.withTempo(session.modeKey(), tempo.bpm()));
        return null;
    }

    private Object control(Consumer<Practice> action) {
        action.accept(current().orElseThrow(
                () -> new RpcException(RpcException.SESSION_ERROR, "No session is running")));
        return null;
    }

    private Optional<Practice> current() {
        return currentSession().map(Active::practice);
    }

    private synchronized Optional<Active> currentSession() {
        return Optional.ofNullable(active);
    }

    private void finished(Active session, Results results) {
        session.input().close();
        session.metronome().ifPresent(OutputConnection::close);
        events.emit("session.summary", SummaryEvent.of(results.summary(), session.practice().elapsedNanos()));
        synchronized (this) {
            if (active == session) {
                active = null;
            }
        }
    }

    private InputConnection openInput(String id, Practice practice, Optional<OutputConnection> metronome,
                                      LatencyOffset latency) {
        try {
            return inputs.open(DeviceId.of(id), event -> {
                events.emit("input", InputMessage.of(event));
                practice.input().onEvent(latency.apply(event));
            });
        } catch (DeviceUnavailableException e) {
            metronome.ifPresent(OutputConnection::close);
            throw new RpcException(RpcException.DEVICE_ERROR, e.getMessage());
        }
    }

    private OutputConnection openOutput(String id) {
        try {
            return outputs.open(DeviceId.of(id));
        } catch (DeviceUnavailableException e) {
            throw new RpcException(RpcException.DEVICE_ERROR, e.getMessage());
        }
    }
}
