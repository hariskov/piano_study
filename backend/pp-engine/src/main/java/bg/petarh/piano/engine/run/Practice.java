package bg.petarh.piano.engine.run;

import bg.petarh.piano.engine.evaluation.FollowMode;
import bg.petarh.piano.engine.metronome.ClickOutput;
import bg.petarh.piano.engine.metronome.Metronome;
import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.engine.time.Clock;
import bg.petarh.piano.midi.input.InputListener;
import bg.petarh.piano.model.time.Tempo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public final class Practice implements AutoCloseable {

    private static final Duration TICK = Duration.ofMillis(5);

    private final Session session;
    private final SessionThread thread;

    private Practice(Session session, SessionThread thread) {
        this.session = session;
        this.thread = thread;
    }

    public static Practice prepare(TargetSource targets, SessionTiming timing, FollowMode mode, Clock clock,
                                   SessionListener listener, Optional<ClickOutput> clicks) {
        Session session = new Session(targets, timing, clock);
        List<SessionListener> listeners = new ArrayList<>(List.of(listener));
        clicks.ifPresent(output -> listeners.add(new Metronome(session, output)));
        SessionDriver driver = new SessionDriver(session, mode.evaluatorFor(session), clock,
                SessionListener.all(listeners));
        return new Practice(session, new SessionThread(driver, TICK));
    }

    public InputListener input() {
        return thread;
    }

    public CompletionStage<Void> start() {
        return thread.start();
    }

    public void pause() {
        thread.pause();
    }

    public void resume() {
        thread.resume();
    }

    public void changeTempo(Tempo tempo) {
        thread.changeTempo(tempo);
    }

    public void stop() {
        thread.close();
    }

    public long elapsedNanos() {
        return session.elapsedNanos();
    }

    @Override
    public void close() {
        stop();
    }
}
