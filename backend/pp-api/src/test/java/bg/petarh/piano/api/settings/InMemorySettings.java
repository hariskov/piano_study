package bg.petarh.piano.api.settings;

import bg.petarh.piano.library.settings.Settings;
import bg.petarh.piano.library.settings.SettingsStore;

public final class InMemorySettings implements SettingsStore {

    private volatile Settings saved = Settings.defaults();
    private volatile int saves;

    @Override
    public Settings load() {
        return saved;
    }

    @Override
    public void save(Settings settings) {
        saved = settings;
        saves++;
    }

    public Settings saved() {
        return saved;
    }

    public int saves() {
        return saves;
    }
}
