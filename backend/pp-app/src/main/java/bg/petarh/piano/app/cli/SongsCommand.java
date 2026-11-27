package bg.petarh.piano.app.cli;

import bg.petarh.piano.library.songs.SongEntry;
import bg.petarh.piano.library.songs.SongLibrary;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(name = "songs", mixinStandardHelpOptions = true, description = "List the songs in the library.")
final class SongsCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Option(names = "--library", paramLabel = "DIR", description = "Song library folder (default: ~/.local/share/piano-player/library).")
    private Optional<Path> library;

    SongsCommand() {
    }

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        List<SongEntry> songs = new SongLibrary(library.orElseGet(SongLibrary::defaultFolder), Instant::now).list();
        if (songs.isEmpty()) {
            out.println("No songs yet. Add one with: pp import <file>");
        }
        for (SongEntry song : songs) {
            out.println(song.id() + "  " + song.title() + "  " + ImportCommand.plural(song.partCount(), "part") + "  "
                    + ImportCommand.plural(song.measureCount(), "measure") + "  " + song.format());
        }
        return 0;
    }
}
