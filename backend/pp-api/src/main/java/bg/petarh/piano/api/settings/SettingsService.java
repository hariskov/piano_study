package bg.petarh.piano.api.settings;

import bg.petarh.piano.library.settings.Settings;
import bg.petarh.piano.library.settings.SettingsStore;

import java.util.function.UnaryOperator;

public final class SettingsService {

    private final SettingsStore store;
    private Settings current;

    public SettingsService(SettingsStore store) {
        this.store = store;
        this.current = store.load();
    }

    public synchronized Settings current() {
        return current;
    }

    public synchronized Settings update(UnaryOperator<Settings> change) {
        Settings changed = change.apply(current);
        if (!changed.equals(current)) {
            store.save(changed);
            current = changed;
        }
        return current;
    }
}
