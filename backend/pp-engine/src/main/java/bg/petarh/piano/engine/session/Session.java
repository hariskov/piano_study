package bg.petarh.piano.engine.session;

import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.engine.time.Clock;
import bg.petarh.piano.engine.time.Stopwatch;
import bg.petarh.piano.model.note.Target;

import java.util.List;
import java.util.Optional;

public final class Session {

    private final TargetSource targets;
    private final SessionTiming timing;
    private final Clock clock;
    private final Stopwatch stopwatch;
    private SessionState state = SessionState.READY;
    private Optional<Target> current = Optional.empty();
    private long countInStartNanos;

    public Session(TargetSource targets, SessionTiming timing, Clock clock) {
        this.targets = targets;
        this.timing = timing;
        this.clock = clock;
        this.stopwatch = new Stopwatch(clock);
    }

    public SessionState state() {
        return state;
    }

    public SessionTiming timing() {
        return timing;
    }

    public Optional<Target> currentTarget() {
        return current;
    }

    public List<Target> upcoming(int count) {
        return targets.peek(count);
    }

    public long elapsedNanos() {
        return stopwatch.elapsedNanos();
    }

    public void start() {
        require(SessionState.READY, "start");
        current = targets.next();
        if (current.isEmpty()) {
            state = SessionState.FINISHED;
        } else if (timing.countInBars() > 0) {
            countInStartNanos = clock.nowNanos();
            state = SessionState.COUNT_IN;
        } else {
            run();
        }
    }

    public void tick() {
        if (state == SessionState.COUNT_IN && clock.nowNanos() - countInStartNanos >= timing.countInNanos()) {
            run();
        }
    }

    public void pause() {
        require(SessionState.RUNNING, "pause");
        stopwatch.pause();
        state = SessionState.PAUSED;
    }

    public void resume() {
        require(SessionState.PAUSED, "resume");
        stopwatch.resume();
        state = SessionState.RUNNING;
    }

    public void advance() {
        require(SessionState.RUNNING, "advance");
        current = targets.next();
        if (current.isEmpty()) {
            finish();
        }
    }

    public void stop() {
        if (state != SessionState.FINISHED) {
            finish();
        }
    }

    private void run() {
        stopwatch.start();
        state = SessionState.RUNNING;
    }

    private void finish() {
        if (stopwatch.isRunning()) {
            stopwatch.pause();
        }
        current = Optional.empty();
        state = SessionState.FINISHED;
    }

    private void require(SessionState expected, String action) {
        if (state != expected) {
            throw new IllegalStateException("Cannot " + action + " a session that is " + state);
        }
    }
}
