package bg.petarh.piano.engine.time;

public final class Stopwatch {

    private enum State { NOT_STARTED, RUNNING, PAUSED }

    private final Clock clock;
    private State state = State.NOT_STARTED;
    private long elapsedBeforeLastStart;
    private long lastStartNanos;

    public Stopwatch(Clock clock) {
        this.clock = clock;
    }

    public void start() {
        startAt(clock.nowNanos());
    }

    public void startAt(long timestampNanos) {
        require(State.NOT_STARTED, "start");
        runFrom(timestampNanos);
    }

    public void pause() {
        require(State.RUNNING, "pause");
        elapsedBeforeLastStart = elapsedNanos();
        state = State.PAUSED;
    }

    public void resume() {
        require(State.PAUSED, "resume");
        runFrom(clock.nowNanos());
    }

    public boolean isRunning() {
        return state == State.RUNNING;
    }

    public long elapsedNanos() {
        return elapsedAt(clock.nowNanos());
    }

    public long elapsedAt(long timestampNanos) {
        return state == State.RUNNING
                ? elapsedBeforeLastStart + timestampNanos - lastStartNanos
                : elapsedBeforeLastStart;
    }

    private void runFrom(long timestampNanos) {
        lastStartNanos = timestampNanos;
        state = State.RUNNING;
    }

    private void require(State expected, String action) {
        if (state != expected) {
            throw new IllegalStateException("Cannot " + action + " a stopwatch that is " + state);
        }
    }
}
