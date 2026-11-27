package bg.petarh.piano.app.lifetime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface Lifetime {

    void keepOpenUntil(AutoCloseable resource, CompletionStage<?> done) throws Exception;

    default void keepOpenUntilStopped(AutoCloseable resource) throws Exception {
        keepOpenUntil(resource, new CompletableFuture<>());
    }
}
