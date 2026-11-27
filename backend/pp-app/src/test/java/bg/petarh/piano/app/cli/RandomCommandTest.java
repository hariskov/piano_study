package bg.petarh.piano.app.cli;

import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class RandomCommandTest {

    private final VirtualKeyboard keyboard = VirtualKeyboard.withClock(System::nanoTime);
    private final ScriptedLifetime lifetime = new ScriptedLifetime();
    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    @Test
    void playsAWaitSessionToTheEnd() {
        lifetime.whileOpen(() -> {
            for (int i = 1; i <= 3; i++) {
                String target = awaitLine("Play: ", i);
                keyboard.press(Pitch.parse(target), Velocity.of(64));
                keyboard.release(Pitch.parse(target));
            }
            awaitLine("Finished.", 1);
        });

        int exitCode = run("random", "virtual:keyboard", "--notes", "3", "--seed", "7", "--count-in", "0");

        assertThat(exitCode).isZero();
        assertThat(out.toString())
                .contains("Seed 7", "Go!", "  ok ")
                .contains("Done: 3 targets, 0 wrong keys, accuracy 100%");
    }

    @Test
    void reportsWrongKeys() {
        lifetime.whileOpen(() -> {
            Pitch target = Pitch.parse(awaitLine("Play: ", 1));
            keyboard.press(target.transpose(1), Velocity.of(64));
            keyboard.press(target, Velocity.of(64));
            awaitLine("Finished.", 1);
        });

        run("random", "virtual:keyboard", "--notes", "1", "--seed", "3", "--count-in", "0");

        assertThat(out.toString()).contains("  wrong ", "Done: 1 target, 1 wrong key");
    }

    @Test
    void reportsMissedNotesInTempoMode() {
        lifetime.whileOpen(() -> awaitLine("Finished.", 1));

        run("random", "virtual:keyboard", "--mode", "tempo", "--tempo", "240", "--notes", "1", "--count-in", "0");

        assertThat(out.toString()).contains("  missed ", "missed 1");
    }

    @Test
    void rejectsSettingsThatCannotBePlayed() {
        int exitCode = run("random", "virtual:keyboard", "--range", "C#4-D#4");

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("fewer than 2 notes");
    }

    @Test
    void rejectsTempoOutOfRange() {
        int exitCode = run("random", "virtual:keyboard", "--tempo", "500");

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("Tempo out of range");
    }

    @Test
    void rejectsUnknownKey() {
        int exitCode = run("random", "virtual:keyboard", "--key", "H");

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("--key");
    }

    private int run(String... args) {
        CliEnvironment environment = new CliEnvironment(
                keyboard.port(), SilentOutput.create().port(), System::nanoTime, lifetime);
        return PianoCli.create(environment, new PrintWriter(out, true), new PrintWriter(err, true)).execute(args);
    }

    private String awaitLine(String prefix, int occurrence) {
        while (true) {
            List<String> matching = out.toString().lines().filter(line -> line.startsWith(prefix)).toList();
            if (matching.size() >= occurrence) {
                return matching.get(occurrence - 1).substring(prefix.length()).trim();
            }
            Thread.onSpinWait();
        }
    }
}
