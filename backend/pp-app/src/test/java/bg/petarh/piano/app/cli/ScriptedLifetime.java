package bg.petarh.piano.app.cli;

import bg.petarh.piano.app.lifetime.Lifetime;

final class ScriptedLifetime implements Lifetime {

    private Runnable script = () -> {
    };

    void whileOpen(Runnable script) {
        this.script = script;
    }

    @Override
    public void keepOpenUntilStopped(AutoCloseable resource) throws Exception {
        try (resource) {
            script.run();
        }
    }
}
