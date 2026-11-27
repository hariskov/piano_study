package bg.petarh.piano.app.cli;

import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class ChordsCommandTest {

    private static final Pattern NOTES = Pattern.compile("\\(([^)]*)\\)");

    private final VirtualKeyboard keyboard = VirtualKeyboard.withClock(System::nanoTime);
    private final ScriptedLifetime lifetime = new ScriptedLifetime();
    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    @Test
    void playsAChordSessionToTheEnd() {
        lifetime.whileOpen(() -> {
            for (int i = 1; i <= 3; i++) {
                List<Pitch> chord = notesIn(awaitLine("Play: ", i));
                chord.forEach(pitch -> keyboard.press(pitch, Velocity.of(64)));
                chord.forEach(keyboard::release);
            }
            awaitLine("Finished.", 1);
        });

        int exitCode = run("chords", "virtual:keyboard", "--types", "maj,min7", "--inversions", "0,1",
                "--notes", "3", "--seed", "11", "--count-in", "0");

        assertThat(exitCode).isZero();
        assertThat(out.toString()).contains("Chords maj, min7", "Done: 3 targets, 0 wrong keys");
    }

    @Test
    void namesEachChordWithItsNotes() {
        lifetime.whileOpen(() -> {
            awaitLine("Play: ", 1);
            awaitLine("Finished.", 0);
        });

        run("chords", "virtual:keyboard", "--types", "maj", "--key", "C", "--notes", "1", "--seed", "1",
                "--count-in", "0");

        assertThat(firstPlayLine()).matches("Play: [A-G][#b]?(/[A-G][#b]?)? {2}\\(([A-G][#b]?\\d ?){3}\\)");
    }

    @Test
    void canHideTheNotes() {
        lifetime.whileOpen(() -> awaitLine("Play: ", 1));

        run("chords", "virtual:keyboard", "--types", "min", "--show", "name", "--notes", "1", "--count-in", "0");

        assertThat(firstPlayLine()).matches("Play: [A-G][#b]?m(/[A-G][#b]?)?");
    }

    @Test
    void canHideTheName() {
        lifetime.whileOpen(() -> awaitLine("Play: ", 1));

        run("chords", "virtual:keyboard", "--show", "notes", "--notes", "1", "--count-in", "0");

        assertThat(firstPlayLine()).matches("Play: ([A-G][#b]?\\d ?){3}");
    }

    @Test
    void rejectsUnknownChordType() {
        int exitCode = run("chords", "virtual:keyboard", "--types", "maj,sus4");

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("--types", "sus4");
    }

    @Test
    void rejectsAnInversionNoChosenChordHas() {
        int exitCode = run("chords", "virtual:keyboard", "--types", "maj", "--inversions", "3");

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("No chosen chord fits");
    }

    private int run(String... args) {
        CliEnvironment environment = new CliEnvironment(
                keyboard.port(), SilentOutput.create().port(), System::nanoTime, lifetime);
        return PianoCli.create(environment, new PrintWriter(out, true), new PrintWriter(err, true)).execute(args);
    }

    private String firstPlayLine() {
        return out.toString().lines().filter(line -> line.startsWith("Play: ")).findFirst().orElseThrow();
    }

    private static List<Pitch> notesIn(String line) {
        Matcher matcher = NOTES.matcher(line);
        String notes = matcher.find() ? matcher.group(1) : line;
        return Arrays.stream(notes.trim().split(" ")).map(Pitch::parse).toList();
    }

    private String awaitLine(String prefix, int occurrence) {
        while (true) {
            List<String> matching = out.toString().lines().filter(line -> line.startsWith(prefix)).toList();
            if (matching.size() >= occurrence) {
                return occurrence == 0 ? "" : matching.get(occurrence - 1).substring(prefix.length()).trim();
            }
            Thread.onSpinWait();
        }
    }
}
