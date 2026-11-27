package bg.petarh.piano.app.cli;

import bg.petarh.piano.midi.input.TimestampSource;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;

class PianoCliTest {

    private final VirtualKeyboard keyboard = VirtualKeyboard.withClock(TimestampSource.system());
    private final SilentOutput output = SilentOutput.create();
    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    private int run(String... args) {
        return PianoCli.create(keyboard.port(), output.port(), new PrintWriter(out, true), new PrintWriter(err, true))
                .execute(args);
    }

    @Test
    void devicesCommandListsInputsAndOutputs() {
        int exitCode = run("devices");

        assertThat(exitCode).isZero();
        assertThat(out.toString())
                .contains("Inputs:", "virtual:keyboard", "Outputs:", "virtual:silent");
    }

    @Test
    void requiresASubcommand() {
        int exitCode = run();

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("Missing required subcommand", "devices");
    }

    @Test
    void rejectsUnknownSubcommand() {
        int exitCode = run("dance");

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("dance");
    }

    @Test
    void printsHelp() {
        int exitCode = run("--help");

        assertThat(exitCode).isZero();
        assertThat(out.toString()).contains("Usage: pp", "devices");
    }
}
