package bg.petarh.piano.app.lifetime;

import java.io.PrintWriter;
import java.util.concurrent.CompletionStage;

public final class UntilShutdown implements Lifetime {

    private final PrintWriter err;

    public UntilShutdown(PrintWriter err) {
        this.err = err;
    }

    @Override
    public void keepOpenUntil(AutoCloseable resource, CompletionStage<?> done) throws Exception {
        Thread closeOnShutdown = Thread.ofPlatform().unstarted(() -> close(resource));
        Runtime.getRuntime().addShutdownHook(closeOnShutdown);
        try {
            done.toCompletableFuture().get();
        } finally {
            Runtime.getRuntime().removeShutdownHook(closeOnShutdown);
            resource.close();
        }
    }

    private void close(AutoCloseable resource) {
        try {
            resource.close();
        } catch (Exception e) {
            err.println("Could not close MIDI device cleanly: " + e.getMessage());
        }
    }
}
