package bg.petarh.piano.app.cli;

import bg.petarh.piano.app.lifetime.Lifetime;

import java.util.concurrent.CompletionStage;

final class ScriptedLifetime implements Lifetime {

    private Runnable script = () -> {
    };

    void whileOpen(Runnable script) {
        this.script = script;
    }

    @Override
    public void keepOpenUntil(AutoCloseable resource, CompletionStage<?> done) throws Exception {
        try (resource) {
            script.run();
        }
        if (!done.toCompletableFuture().isDone()) {
            throw new IllegalStateException("Test script ended before the command finished");
        }
    }

    @Override
    public void keepOpenUntilStopped(AutoCloseable resource) throws Exception {
        try (resource) {
            script.run();
        }
    }
}
