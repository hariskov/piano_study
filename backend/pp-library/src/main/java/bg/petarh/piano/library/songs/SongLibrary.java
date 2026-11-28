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
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class SongLibrary {

    private static final String INDEX_FILE = "index.json";
    private static final String XML_FOLDER = "xml";
    private static final String MIDI_FOLDER = "midi";
    private static final String SHEETS_FOLDER = "sheets";
    private static final String XML_EXTENSION = ".musicxml";
    private static final String MIDI_EXTENSION = ".mid";

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
        return base.resolve("piano-player").resolve("musicFiles");
    }

    public synchronized SongEntry importMusicXml(Path file) throws IOException {
        byte[] xml = reader.scoreXml(file);
        String fileName = file.getFileName().toString();
        ImportResult result = reader.read(new ByteArrayInputStream(xml), fileName);
        String format = fileName.toLowerCase(Locale.ROOT).endsWith(".mxl") ? "MusicXML (compressed)" : "MusicXML";
        return store(result.score(), xml, fileName, format, result.warnings(), Optional.empty());
    }

    public synchronized SongEntry add(Score score, String sourceFile, String format, List<String> warnings) {
        return store(score, written(score), sourceFile, format, warnings, Optional.empty());
    }

    public synchronized SongEntry addMidi(Score score, Path midiFile, List<String> warnings) throws IOException {
        return store(score, written(score), midiFile.getFileName().toString(), "MIDI", warnings,
                Optional.of(Files.readAllBytes(midiFile)));
    }

    private byte[] written(Score score) {
        ByteArrayOutputStream xml = new ByteArrayOutputStream();
        writer.write(score, xml);
        return xml.toByteArray();
    }

    public synchronized List<SongEntry> list() {
        return songs().stream()
                .sorted(Comparator.comparing(SongEntry::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public synchronized Optional<SongEntry> find(String name) {
        String wanted = name.strip();
        return songs().stream()
                .filter(song -> song.id().equalsIgnoreCase(wanted) || song.title().equalsIgnoreCase(wanted)
                        || song.fileName().equalsIgnoreCase(wanted))
                .findFirst();
    }

    public synchronized Optional<LoadedSong> load(String id) {
        return songs().stream().filter(song -> song.id().equals(id)).findFirst().map(entry -> {
            try {
                return new LoadedSong(entry, reader.read(songFile(entry)).score());
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read song " + id, e);
            }
        });
    }

    public synchronized boolean delete(String id) {
        List<SongEntry> songs = new ArrayList<>(readIndex());
        Optional<SongEntry> removed = songs.stream().filter(song -> song.id().equals(id)).findFirst();
        if (removed.isEmpty()) {
            return false;
        }
        songs.remove(removed.get());
        writeIndex(songs);
        try {
            Files.deleteIfExists(songFile(removed.get()));
            if (removed.get().midiFile().isPresent()) {
                Files.deleteIfExists(folder.resolve(MIDI_FOLDER).resolve(removed.get().midiFile().get()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not delete " + removed.get().fileName(), e);
        }
        return true;
    }

    private SongEntry store(Score score, byte[] xml, String sourceFile, String format, List<String> warnings,
                            Optional<byte[]> midi) {
        List<SongEntry> songs = new ArrayList<>(readIndex());
        String base = freeBaseName(score.title());
        SongEntry entry = new SongEntry(freeId(songs, slug(score.title())), score.title(), base + XML_EXTENSION,
                midi.map(bytes -> base + MIDI_EXTENSION), score.parts().size(), score.measureCount(), sourceFile,
                format, clock.get(), warnings);
        try {
            for (String subfolder : List.of(XML_FOLDER, MIDI_FOLDER, SHEETS_FOLDER)) {
                Files.createDirectories(folder.resolve(subfolder));
            }
            Files.write(songFile(entry), xml);
            if (midi.isPresent()) {
                Files.write(folder.resolve(MIDI_FOLDER).resolve(entry.midiFile().orElseThrow()), midi.get());
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + entry.fileName(), e);
        }
        songs.add(entry);
        writeIndex(songs);
        return entry;
    }

    public Path xmlPath(SongEntry song) {
        return songFile(song);
    }

    public Optional<Path> midiPath(SongEntry song) {
        return song.midiFile().map(midi -> folder.resolve(MIDI_FOLDER).resolve(midi));
    }

    private List<SongEntry> songs() {
        return readIndex().stream().filter(song -> Files.isRegularFile(songFile(song))).toList();
    }

    private List<SongEntry> readIndex() {
        Path index = folder.resolve(INDEX_FILE);
        if (!Files.isRegularFile(index)) {
            return List.of();
        }
        try {
            return json.readValue(index.toFile(), Index.class).songs().stream().map(Index.Song::toEntry).toList();
        } catch (RuntimeException e) {
            moveAside(index);
            return List.of();
        }
    }

    private void writeIndex(List<SongEntry> songs) {
        try {
            Files.createDirectories(folder);
            Path temporary = Files.createTempFile(folder, INDEX_FILE, ".tmp");
            try {
                json.writeValue(temporary.toFile(), new Index(songs.stream().map(Index.Song::of).toList()));
                Files.move(temporary, folder.resolve(INDEX_FILE), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save the song library index", e);
        }
    }

    private static void moveAside(Path index) {
        try {
            Files.move(index, index.resolveSibling(INDEX_FILE + ".bad"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not move the unreadable library index aside", e);
        }
    }

    private Path songFile(SongEntry song) {
        return folder.resolve(XML_FOLDER).resolve(song.fileName());
    }

    private String freeBaseName(String title) {
        String base = safeFileName(title);
        String name = base;
        for (int number = 2; isUsed(name); number++) {
            name = base + " (" + number + ")";
        }
        return name;
    }

    private boolean isUsed(String baseName) {
        return Files.exists(folder.resolve(XML_FOLDER).resolve(baseName + XML_EXTENSION))
                || Files.exists(folder.resolve(MIDI_FOLDER).resolve(baseName + MIDI_EXTENSION));
    }

    private static String freeId(List<SongEntry> songs, String base) {
        String id = base;
        for (int number = 2; isTaken(songs, id); number++) {
            id = base + "-" + number;
        }
        return id;
    }

    private static boolean isTaken(List<SongEntry> songs, String id) {
        return songs.stream().anyMatch(song -> song.id().equals(id));
    }

    static String slug(String title) {
        String plain = Normalizer.normalize(title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = plain.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? "song" : slug;
    }

    static String safeFileName(String title) {
        String safe = title.replaceAll("[/\\\\:*?\"<>|\\p{Cntrl}]", "-").strip().replaceAll("^\\.+", "");
        return safe.isEmpty() ? "Song" : safe;
    }
}
