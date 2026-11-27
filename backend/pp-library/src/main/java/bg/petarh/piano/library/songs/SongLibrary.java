package bg.petarh.piano.library.songs;

import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.musicxml.ImportResult;
import bg.petarh.piano.musicxml.MusicXmlReader;
import bg.petarh.piano.musicxml.MusicXmlWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class SongLibrary {

    private static final String SCORE_FILE = "score.musicxml";
    private static final String META_FILE = "meta.json";
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9-]*");

    private final Path folder;
    private final Supplier<Instant> clock;
    private final MusicXmlReader reader = new MusicXmlReader();
    private final MusicXmlWriter writer = new MusicXmlWriter();
    private final JsonMapper json = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    public SongLibrary(Path folder, Supplier<Instant> clock) {
        this.folder = folder;
        this.clock = clock;
    }

    public static Path defaultFolder() {
        return defaultFolder(System.getenv(), Path.of(System.getProperty("user.home")));
    }

    static Path defaultFolder(Map<String, String> environment, Path home) {
        String dataHome = environment.getOrDefault("XDG_DATA_HOME", "");
        Path base = dataHome.isBlank() ? home.resolve(".local").resolve("share") : Path.of(dataHome);
        return base.resolve("piano-player").resolve("library");
    }

    public SongEntry importMusicXml(Path file) throws IOException {
        byte[] xml = reader.scoreXml(file);
        String fileName = file.getFileName().toString();
        ImportResult result = reader.read(new ByteArrayInputStream(xml), fileName);
        String format = fileName.toLowerCase(Locale.ROOT).endsWith(".mxl") ? "MusicXML (compressed)" : "MusicXML";
        return store(result.score(), xml, fileName, format, result.warnings());
    }

    public SongEntry add(Score score, String sourceFile, String format, List<String> warnings) {
        ByteArrayOutputStream xml = new ByteArrayOutputStream();
        writer.write(score, xml);
        return store(score, xml.toByteArray(), sourceFile, format, warnings);
    }

    public List<SongEntry> list() {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> songs = Files.list(folder)) {
            return songs.filter(Files::isDirectory)
                    .flatMap(song -> entry(song.getFileName().toString()).stream())
                    .sorted(Comparator.comparing(SongEntry::title, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list the song library " + folder, e);
        }
    }

    public Optional<LoadedSong> load(String id) {
        return entry(id).map(entry -> {
            try {
                return new LoadedSong(entry, reader.read(songFolder(id).resolve(SCORE_FILE)).score());
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read song " + id, e);
            }
        });
    }

    public boolean delete(String id) {
        if (entry(id).isEmpty()) {
            return false;
        }
        deleteFolder(songFolder(id));
        return true;
    }

    private SongEntry store(Score score, byte[] xml, String sourceFile, String format, List<String> warnings) {
        String id = freeId(slug(score.title()));
        SongEntry entry = new SongEntry(id, score.title(), score.parts().size(), score.measureCount(), sourceFile, format,
                clock.get(), warnings);
        Path song = songFolder(id);
        try {
            Files.createDirectories(song);
            Files.write(song.resolve(SCORE_FILE), xml);
            json.writeValue(song.resolve(META_FILE).toFile(), MetaFile.of(entry));
            return entry;
        } catch (IOException | RuntimeException e) {
            deleteFolder(song);
            throw e instanceof IOException io ? new UncheckedIOException("Could not store " + sourceFile, io)
                    : (RuntimeException) e;
        }
    }

    private Optional<SongEntry> entry(String id) {
        if (!ID.matcher(id).matches()) {
            return Optional.empty();
        }
        Path meta = songFolder(id).resolve(META_FILE);
        if (!Files.isRegularFile(meta) || !Files.isRegularFile(songFolder(id).resolve(SCORE_FILE))) {
            return Optional.empty();
        }
        try {
            return Optional.of(json.readValue(meta.toFile(), MetaFile.class).toEntry(id));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private String freeId(String base) {
        String id = base;
        for (int number = 2; Files.exists(songFolder(id)); number++) {
            id = base + "-" + number;
        }
        return id;
    }

    private Path songFolder(String id) {
        return folder.resolve(id);
    }

    static String slug(String title) {
        String plain = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? "song" : slug;
    }

    private static void deleteFolder(Path song) {
        if (!Files.exists(song)) {
            return;
        }
        try (Stream<Path> files = Files.walk(song)) {
            for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete " + song, e);
        }
    }
}
