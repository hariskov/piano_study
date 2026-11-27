package bg.petarh.piano.library.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class JsonSettingsFile implements SettingsStore {

    private final Path file;
    private final JsonMapper mapper = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();

    public JsonSettingsFile(Path file) {
        this.file = file;
    }

    @Override
    public Settings load() {
        if (!Files.exists(file)) {
            return Settings.defaults();
        }
        try {
            return mapper.readValue(file.toFile(), SettingsFile.class).toSettings();
        } catch (JacksonException e) {
            moveAside();
            return Settings.defaults();
        }
    }

    @Override
    public void save(Settings settings) {
        try {
            Path folder = file.toAbsolutePath().getParent();
            Files.createDirectories(folder);
            Path written = Files.createTempFile(folder, file.getFileName().toString(), ".tmp");
            try {
                mapper.writeValue(written.toFile(), SettingsFile.of(settings));
                Files.move(written, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(written);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save settings to " + file, e);
        }
    }

    private void moveAside() {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".bad"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not move unreadable settings aside: " + file, e);
        }
    }
}
