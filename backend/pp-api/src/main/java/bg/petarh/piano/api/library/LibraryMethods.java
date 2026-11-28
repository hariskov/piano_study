package bg.petarh.piano.api.library;

import bg.petarh.piano.api.rpc.MayBeAbsent;
import bg.petarh.piano.api.rpc.RpcException;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.importmidi.MidiConversion;
import bg.petarh.piano.importmidi.MidiFileReader;
import bg.petarh.piano.importmidi.MidiImportException;
import bg.petarh.piano.importmidi.MidiImportOptions;
import bg.petarh.piano.importmidi.MidiToScore;
import bg.petarh.piano.library.songs.LoadedSong;
import bg.petarh.piano.library.songs.SongEntry;
import bg.petarh.piano.library.songs.SongLibrary;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.score.ScoreNote;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.musicxml.MusicXmlException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class LibraryMethods {

    public record SongSummary(String id, String title, String fileName, @MayBeAbsent String midiFile, int partCount,
                              int measureCount, String sourceFile, String format, String importedAt,
                              List<String> warnings) {

        static SongSummary of(SongEntry entry) {
            return new SongSummary(entry.id(), entry.title(), entry.fileName(), entry.midiFile().orElse(null),
                    entry.partCount(), entry.measureCount(), entry.sourceFile(), entry.format(),
                    entry.importedAt().toString(), entry.warnings());
        }
    }

    public record SongList(List<SongSummary> songs) {
    }

    public record ImportParams(String path, Integer grid, Boolean triplets, String splitPoint) {
    }

    public record SongId(String id) {
    }

    public record PartSummary(String id, String name, int staffCount, int noteCount, @MayBeAbsent String lowest,
                              @MayBeAbsent String highest) {

        static PartSummary of(Part part) {
            Comparator<ScoreNote> byPitch = Comparator.comparing(ScoreNote::pitch);
            String lowest = part.notes().stream().min(byPitch).map(note -> note.pitch().name()).orElse(null);
            String highest = part.notes().stream().max(byPitch).map(note -> note.pitch().name()).orElse(null);
            return new PartSummary(part.id(), part.name(), part.staffCount(), part.notes().size(), lowest, highest);
        }
    }

    public record SongDetails(String id, String title, @MayBeAbsent String composer, String file,
                              @MayBeAbsent String midiFile, int measureCount, String timeSignature, String key,
                              int tempo, List<PartSummary> parts, List<String> warnings) {
    }

    public record MusicXml(String xml) {
    }

    private LibraryMethods() {
    }

    public static void register(RpcServer server, SongLibrary library) {
        server.register("library.list", Void.class, SongList.class,
                ignored -> new SongList(library.list().stream().map(SongSummary::of).toList()));
        server.register("library.import", ImportParams.class, SongSummary.class,
                params -> SongSummary.of(importFile(library, params)));
        server.register("library.open", SongId.class, SongDetails.class, params -> details(library, params.id()));
        server.register("library.musicXml", SongId.class, MusicXml.class, params -> musicXml(library, params.id()));
        server.register("library.delete", SongId.class, Void.class, params -> {
            if (!library.delete(required(params.id()))) {
                throw notFound(params.id());
            }
            return null;
        });
    }

    private static SongEntry importFile(SongLibrary library, ImportParams params) {
        Path file = Path.of(required(params.path()));
        String name = file.getFileName().toString();
        String lower = name.toLowerCase(Locale.ROOT);
        try {
            if (lower.endsWith(".mid") || lower.endsWith(".midi")) {
                MidiConversion converted = new MidiToScore().convert(new MidiFileReader().read(file), options(params),
                        name.substring(0, name.lastIndexOf('.')));
                return library.addMidi(converted.score(), file, converted.warnings());
            }
            if (lower.endsWith(".musicxml") || lower.endsWith(".xml") || lower.endsWith(".mxl")) {
                return library.importMusicXml(file);
            }
        } catch (IOException | UncheckedIOException | MusicXmlException | MidiImportException e) {
            throw new RpcException(RpcException.FILE_ERROR, name + ": " + describe(e));
        }
        throw new IllegalArgumentException("Choose a MIDI (.mid, .midi) or MusicXML (.musicxml, .xml, .mxl) file: " + name);
    }

    private static MidiImportOptions options(ImportParams params) {
        MidiImportOptions options = MidiImportOptions.defaults();
        if (params.grid() != null) {
            int grid = params.grid();
            if (Integer.bitCount(grid) != 1 || grid < 4 || grid > 32) {
                throw new IllegalArgumentException("grid must be 4, 8, 16 or 32: " + grid);
            }
            options = options.withGrid(Fraction.of(1, grid));
        }
        if (params.triplets() != null) {
            options = options.withTriplets(params.triplets());
        }
        if (params.splitPoint() != null) {
            options = options.withSplitPoint(Pitch.parse(params.splitPoint()).midiNumber());
        }
        return options;
    }

    private static SongDetails details(SongLibrary library, String id) {
        LoadedSong song = library.load(required(id)).orElseThrow(() -> notFound(id));
        Score score = song.score();
        SongEntry entry = song.entry();
        return new SongDetails(entry.id(), entry.title(), score.composer().orElse(null),
                library.xmlPath(entry).toString(), library.midiPath(entry).map(Path::toString).orElse(null),
                score.measureCount(), score.timeSignatureAt(Fraction.ZERO).toString(),
                score.keyAt(Fraction.ZERO).toString(), score.tempoMap().tempoAt(Fraction.ZERO).bpm(),
                score.parts().stream().map(PartSummary::of).toList(), entry.warnings());
    }

    private static MusicXml musicXml(SongLibrary library, String id) {
        SongEntry entry = library.find(required(id)).orElseThrow(() -> notFound(id));
        try {
            return new MusicXml(Files.readString(library.xmlPath(entry)));
        } catch (IOException e) {
            throw new RpcException(RpcException.FILE_ERROR, entry.fileName() + ": " + describe(e));
        }
    }

    private static String describe(Exception e) {
        String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return e instanceof NoSuchFileException ? "the file does not exist" : message;
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A value is missing");
        }
        return value;
    }

    private static RpcException notFound(String id) {
        return new RpcException(RpcException.NOT_FOUND, "No song with id " + id);
    }
}
