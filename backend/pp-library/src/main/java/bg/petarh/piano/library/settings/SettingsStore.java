package bg.petarh.piano.library.settings;

public interface SettingsStore {

    Settings load();

    void save(Settings settings);
}
