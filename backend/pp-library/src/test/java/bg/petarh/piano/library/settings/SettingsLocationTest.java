package bg.petarh.piano.library.settings;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SettingsLocationTest {

    @Test
    void usesTheXdgConfigFolderWhenSet() {
        assertThat(SettingsLocation.settingsFile(Map.of("XDG_CONFIG_HOME", "/tmp/config"), Path.of("/home/ana")))
                .isEqualTo(Path.of("/tmp/config/piano-player/settings.json"));
    }

    @Test
    void fallsBackToDotConfigInTheHomeFolder() {
        assertThat(SettingsLocation.settingsFile(Map.of(), Path.of("/home/ana")))
                .isEqualTo(Path.of("/home/ana/.config/piano-player/settings.json"));
        assertThat(SettingsLocation.settingsFile(Map.of("XDG_CONFIG_HOME", ""), Path.of("/home/ana")))
                .isEqualTo(Path.of("/home/ana/.config/piano-player/settings.json"));
    }
}
