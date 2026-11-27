package bg.petarh.piano.library.settings;

import java.nio.file.Path;
import java.util.Map;

public final class SettingsLocation {

    private static final String APP_FOLDER = "piano-player";
    private static final String FILE_NAME = "settings.json";

    private SettingsLocation() {
    }

    public static Path defaultSettingsFile() {
        return settingsFile(System.getenv(), Path.of(System.getProperty("user.home")));
    }

    static Path settingsFile(Map<String, String> environment, Path home) {
        String configHome = environment.getOrDefault("XDG_CONFIG_HOME", "");
        Path base = configHome.isBlank() ? home.resolve(".config") : Path.of(configHome);
        return base.resolve(APP_FOLDER).resolve(FILE_NAME);
    }
}
