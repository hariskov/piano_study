package bg.petarh.piano.app.lifetime;

import java.io.PrintWriter;
import java.util.concurrent.CountDownLatch;

public final class UntilShutdown implements Lifetime {

    private final PrintWriter err;

    public UntilShutdown(PrintWriter err) {
        this.err = err;
    }

    @Override
    public void keepOpenUntilStopped(AutoCloseable resource) throws InterruptedException {
        Runtime.getRuntime().addShutdownHook(Thread.ofPlatform().unstarted(() -> close(resource)));
        new CountDownLatch(1).await();
    }

    private void close(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception e) {
            err.println("Could not close MIDI device cleanly: " + e.getMessage());
        }
    }
}
