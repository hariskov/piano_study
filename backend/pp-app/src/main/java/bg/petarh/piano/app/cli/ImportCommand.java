package bg.petarh.piano.app.cli;

import bg.petarh.piano.importmidi.MidiConversion;
import bg.petarh.piano.importmidi.MidiFileReader;
import bg.petarh.piano.importmidi.MidiImportOptions;
import bg.petarh.piano.importmidi.MidiToScore;
import bg.petarh.piano.library.songs.SongEntry;
import bg.petarh.piano.library.songs.SongLibrary;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.score.ScoreNote;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.musicxml.MusicXmlWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
        name = "import",
        mixinStandardHelpOptions = true,
        description = "Import a MIDI or MusicXML file into the song library, or convert MIDI to a MusicXML file.")
final class ImportCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Parameters(paramLabel = "FILE", description = "A .mid/.midi, .musicxml/.xml or .mxl file.")
    private Path file;

    @Option(names = "--out", paramLabel = "FILE", description = "Write the converted MIDI file here as MusicXML instead.")
    private Optional<Path> out;

    @Option(names = "--library", paramLabel = "DIR", description = "Song library folder (default: ~/.local/share/piano-player/musicFiles).")
    private Optional<Path> library;

    @Option(names = "--grid", defaultValue = "16", description = "MIDI: snap notes to 1/4, 1/8, 1/16 or 1/32 (default: ${DEFAULT-VALUE}).")
    private int grid;

    @Option(names = "--triplets", description = "MIDI: also snap to triplets.")
    private boolean triplets;

    @Option(names = "--split", defaultValue = "C4", description = "MIDI: lowest note of the right hand for piano parts (default: ${DEFAULT-VALUE}).")
    private String split;

    ImportCommand() {
    }

    @Override
    public Integer call() throws Exception {
        PrintWriter printer = spec.commandLine().getOut();
        String name = file.getFileName().toString();
        String lower = name.toLowerCase(Locale.ROOT);
        SongLibrary songs = new SongLibrary(library.orElseGet(SongLibrary::defaultFolder), Instant::now);
        if (lower.endsWith(".mid") || lower.endsWith(".midi")) {
            MidiConversion converted = new MidiToScore().convert(new MidiFileReader().read(file), options(),
                    name.substring(0, name.lastIndexOf('.')));
            if (out.isPresent()) {
                try (OutputStream stream = Files.newOutputStream(out.get())) {
                    new MusicXmlWriter().write(converted.score(), stream);
                }
                printer.println("Wrote " + out.get());
                describe(printer, converted.score(), converted.warnings());
                return 0;
            }
            SongEntry entry = songs.addMidi(converted.score(), file, converted.warnings());
            report(printer, entry, converted.score());
            return 0;
        }
        if (out.isPresent()) {
            throw new ParameterException(spec.commandLine(), "--out is only for converting MIDI files");
        }
        SongEntry entry = songs.importMusicXml(file);
        report(printer, entry, songs.load(entry.id()).orElseThrow().score());
        return 0;
    }

    private MidiImportOptions options() {
        if (Integer.bitCount(grid) != 1 || grid < 4 || grid > 32) {
            throw new ParameterException(spec.commandLine(), "--grid must be 4, 8, 16 or 32");
        }
        try {
            return MidiImportOptions.defaults().withGrid(Fraction.of(1, grid)).withTriplets(triplets)
                    .withSplitPoint(Pitch.parse(split).midiNumber());
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), e.getMessage(), e);
        }
    }

    private static void report(PrintWriter printer, SongEntry entry, Score score) {
        printer.println("Imported \"" + entry.title() + "\" as " + entry.id() + " (" + entry.format() + ", "
                + plural(entry.partCount(), "part") + ", " + plural(entry.measureCount(), "measure") + ").");
        describe(printer, score, entry.warnings());
    }

    private static void describe(PrintWriter printer, Score score, List<String> warnings) {
        for (Part part : score.parts()) {
            printer.println(describePart(part));
        }
        if (!warnings.isEmpty()) {
            printer.println("Warnings:");
            warnings.forEach(warning -> printer.println("  - " + warning));
        }
    }

    static String describePart(Part part) {
        return "  " + part.id() + "  " + part.name() + "  " + plural(part.staffCount(), "staff", "staves")
                + "  " + plural(part.notes().size(), "note") + range(part.notes());
    }

    private static String range(List<ScoreNote> notes) {
        if (notes.isEmpty()) {
            return "";
        }
        Comparator<ScoreNote> byPitch = Comparator.comparing(ScoreNote::pitch);
        return "  " + notes.stream().min(byPitch).orElseThrow().pitch().name() + "-"
                + notes.stream().max(byPitch).orElseThrow().pitch().name();
    }

    static String plural(int count, String noun) {
        return plural(count, noun, noun + "s");
    }

    private static String plural(int count, String one, String many) {
        return count + " " + (count == 1 ? one : many);
    }
}
