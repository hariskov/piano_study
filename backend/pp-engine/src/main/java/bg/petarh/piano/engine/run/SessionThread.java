package bg.petarh.piano.engine.run;

import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.input.InputListener;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class SessionThread implements InputListener, AutoCloseable {

    private final SessionDriver driver;
    private final long tickNanos;
    private final BlockingQueue<Runnable> actions = new LinkedBlockingQueue<>();
    private final CompletableFuture<Void> done = new CompletableFuture<>();
    private Thread thread;

    public SessionThread(SessionDriver driver, Duration tickInterval) {
        this.driver = driver;
        this.tickNanos = tickInterval.toNanos();
    }

    public synchronized CompletionStage<Void> start() {
        if (thread != null) {
            throw new IllegalStateException("Session thread already started");
        }
        thread = Thread.ofPlatform().name("session").daemon().start(this::run);
        return done;
    }

    @Override
    public void onEvent(InputEvent event) {
        actions.add(() -> driver.handle(event));
    }

    public void pause() {
        actions.add(driver::pause);
    }

    public void resume() {
        actions.add(driver::resume);
    }

    @Override
    public void close() {
        Thread running;
        synchronized (this) {
            running = thread;
        }
        if (running != null) {
            actions.add(driver::stop);
            try {
                running.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void run() {
        try {
            driver.start();
            while (!driver.isFinished()) {
                Runnable action = actions.poll(tickNanos, TimeUnit.NANOSECONDS);
                if (action != null) {
                    action.run();
                }
                driver.tick();
            }
            done.complete(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            done.completeExceptionally(e);
        } catch (RuntimeException e) {
            done.completeExceptionally(e);
        }
    }
}
