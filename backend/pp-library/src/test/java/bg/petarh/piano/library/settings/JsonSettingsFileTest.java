package bg.petarh.piano.library.settings;

import bg.petarh.piano.model.keyboard.KeyboardRange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JsonSettingsFileTest {

    @TempDir
    Path directory;

    @Test
    void startsWithDefaultsWhenThereIsNoFile() {
        assertThat(store().load()).isEqualTo(Settings.defaults());
    }

    @Test
    void savesAndLoadsEverySetting() {
        Settings settings = Settings.defaults().withInput("jdk:Piano [hw:3,0,0]").withOutput("jdk:Gervill")
                .withKeyboard("jdk:Piano [hw:3,0,0]", KeyboardRange.parse("F1-F7"))
                .withTempo("wait", 72).withTempo("tempo", 100).withLatencyOffset(15).withInstrument(56).withKeySound(true);

        store().save(settings);

        assertThat(store().load()).isEqualTo(settings);
    }

    @Test
    void createsTheFolderWhenSaving() {
        JsonSettingsFile store = new JsonSettingsFile(directory.resolve("piano-player/settings.json"));

        store.save(Settings.defaults().withInput("jdk:Piano"));

        assertThat(directory.resolve("piano-player/settings.json")).exists();
    }

    @Test
    void writesReadableJson() throws Exception {
        store().save(Settings.defaults().withInput("jdk:Piano").withTempo("wait", 72));

        assertThat(Files.readString(file())).contains("\"input\" : \"jdk:Piano\"", "\"wait\" : 72");
    }

    @Test
    void leavesNoTemporaryFilesBehind() throws Exception {
        store().save(Settings.defaults());

        try (var files = Files.list(directory)) {
            assertThat(files).containsExactly(file());
        }
    }

    @Test
    void movesACorruptFileAsideAndStartsWithDefaults() throws Exception {
        Files.writeString(file(), "{ not json");

        assertThat(store().load()).isEqualTo(Settings.defaults());
        assertThat(directory.resolve("settings.json.bad")).hasContent("{ not json");
        assertThat(file()).doesNotExist();
    }

    @Test
    void ignoresSettingsItDoesNotKnowAndValuesItCannotUse() throws Exception {
        Files.writeString(file(), """
                {"input":"jdk:Piano","theme":"dark","keyboards":{"jdk:Piano":"nonsense","jdk:Other":"C2-C7"},
                 "latencyOffsetMs":9999,"instrument":300}""");

        Settings settings = store().load();

        assertThat(settings.input()).contains("jdk:Piano");
        assertThat(settings.keyboardFor("jdk:Piano")).isEmpty();
        assertThat(settings.keyboardFor("jdk:Other")).contains(KeyboardRange.parse("C2-C7"));
        assertThat(settings.latencyOffsetMs()).isZero();
        assertThat(settings.instrument()).isZero();
    }

    private JsonSettingsFile store() {
        return new JsonSettingsFile(file());
    }

    private Path file() {
        return directory.resolve("settings.json");
    }
}
