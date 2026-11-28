package bg.petarh.piano.library.progress;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class ProgressLog implements SessionHistory {

    public static final int KEPT_SESSIONS = 500;

    private static final Pattern PLAIN_NAME = Pattern.compile("[a-z0-9][a-z0-9-]*");
    private static final TypeReference<List<SessionRecord>> SESSIONS = new TypeReference<>() {
    };

    private final Path folder;
    private final JsonMapper json = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_NULL))
            .build();

    public ProgressLog(Path folder) {
        this.folder = folder;
    }

    @Override
    public synchronized void record(SessionRecord session) {
        List<SessionRecord> sessions = new ArrayList<>(history(session.key()));
        sessions.add(session);
        write(file(session.key()), sessions.subList(Math.max(0, sessions.size() - KEPT_SESSIONS), sessions.size()));
    }

    @Override
    public synchronized List<SessionRecord> history(String key) {
        Path file = file(key);
        if (!Files.exists(file)) {
            return List.of();
        }
        try {
            return List.copyOf(json.readValue(file.toFile(), SESSIONS));
        } catch (JacksonException e) {
            moveAside(file);
            return List.of();
        }
    }

    private Path file(String key) {
        if (!PLAIN_NAME.matcher(key).matches()) {
            throw new IllegalArgumentException("Not a progress name: " + key);
        }
        return folder.resolve(key + ".json");
    }

    private void write(Path file, List<SessionRecord> sessions) {
        try {
            Files.createDirectories(folder);
            Path temporary = Files.createTempFile(folder, file.getFileName().toString(), ".tmp");
            try {
                json.writeValue(temporary.toFile(), sessions);
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save progress to " + file, e);
        }
    }

    private static void moveAside(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".bad"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not move unreadable progress aside: " + file, e);
        }
    }
}
