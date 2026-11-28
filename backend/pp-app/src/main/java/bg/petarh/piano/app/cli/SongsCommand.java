package bg.petarh.piano.app.cli;

import bg.petarh.piano.library.songs.LoadedSong;
import bg.petarh.piano.library.songs.SongEntry;
import bg.petarh.piano.library.songs.SongLibrary;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.time.Fraction;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(name = "songs", mixinStandardHelpOptions = true,
        description = "List the song library, open a song by name, or delete one.")
final class SongsCommand implements Callable<Integer> {

    private static final int NOT_FOUND = 1;

    @Spec
    private CommandSpec spec;

    @Parameters(arity = "0..1", paramLabel = "NAME", description = "Show this song (title, id or file name).")
    private Optional<String> name;

    @Option(names = "--delete", paramLabel = "NAME", description = "Delete this song from the library.")
    private Optional<String> delete;

    @Option(names = "--library", paramLabel = "DIR", description = "Song library folder (default: ~/.local/share/piano-player/musicFiles).")
    private Optional<Path> library;

    SongsCommand() {
    }

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        Path folder = library.orElseGet(SongLibrary::defaultFolder);
        SongLibrary songs = new SongLibrary(folder, Instant::now);
        if (delete.isPresent()) {
            return found(songs, delete.get()).map(song -> {
                songs.delete(song.id());
                out.println("Deleted \"" + song.title() + "\".");
                return 0;
            }).orElse(NOT_FOUND);
        }
        if (name.isPresent()) {
            return found(songs, name.get()).map(song -> {
                show(out, songs.load(song.id()).orElseThrow(), folder);
                return 0;
            }).orElse(NOT_FOUND);
        }
        List<SongEntry> entries = songs.list();
        if (entries.isEmpty()) {
            out.println("No songs yet. Add one with: pp import <file>");
        }
        for (SongEntry song : entries) {
            out.println(song.id() + "  " + song.title() + "  " + ImportCommand.plural(song.partCount(), "part") + "  "
                    + ImportCommand.plural(song.measureCount(), "measure") + "  " + song.format());
        }
        return 0;
    }

    private Optional<SongEntry> found(SongLibrary songs, String wanted) {
        Optional<SongEntry> song = songs.find(wanted);
        if (song.isEmpty()) {
            spec.commandLine().getErr().println("No song called \"" + wanted + "\". Run 'pp songs' to see the library.");
        }
        return song;
    }

    private static void show(PrintWriter out, LoadedSong loaded, Path folder) {
        SongEntry entry = loaded.entry();
        Score score = loaded.score();
        out.println(entry.title() + score.composer().map(composer -> " (" + composer + ")").orElse(""));
        out.println("File: " + folder.resolve("xml").resolve(entry.fileName()));
        entry.midiFile().ifPresent(midi -> out.println("MIDI: " + folder.resolve("midi").resolve(midi)));
        out.println("From: " + entry.sourceFile() + " (" + entry.format() + "), imported " + entry.importedAt());
        out.println(ImportCommand.plural(score.measureCount(), "measure") + ", " + score.timeSignatureAt(Fraction.ZERO)
                + ", " + score.keyAt(Fraction.ZERO) + ", " + score.tempoMap().tempoAt(Fraction.ZERO).bpm() + " BPM");
        for (Part part : score.parts()) {
            out.println(ImportCommand.describePart(part));
        }
        if (!entry.warnings().isEmpty()) {
            out.println("Warnings:");
            entry.warnings().forEach(warning -> out.println("  - " + warning));
        }
    }
}
