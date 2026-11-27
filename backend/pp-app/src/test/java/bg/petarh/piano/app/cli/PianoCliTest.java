package bg.petarh.piano.app.cli;

import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class PianoCliTest {

    private static final Pitch MIDDLE_C = Pitch.parse("C4");

    private final AtomicLong now = new AtomicLong(0L);
    private final VirtualKeyboard keyboard = VirtualKeyboard.withClock(now::get);
    private final SilentOutput output = SilentOutput.create();
    private final ScriptedLifetime lifetime = new ScriptedLifetime();
    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    private int run(String... args) {
        CliEnvironment environment = new CliEnvironment(keyboard.port(), output.port(), now::get, lifetime);
        return PianoCli.create(environment, new PrintWriter(out, true), new PrintWriter(err, true)).execute(args);
    }

    @Test
    void devicesCommandListsInputsAndOutputs() {
        int exitCode = run("devices");

        assertThat(exitCode).isZero();
        assertThat(out.toString())
                .contains("Inputs:", "virtual:keyboard", "Outputs:", "virtual:silent");
    }

    @Test
    void monitorPrintsEveryKeyEventUntilStopped() {
        lifetime.whileOpen(() -> {
            now.set(500_000_000L);
            keyboard.press(MIDDLE_C, Velocity.of(80));
            now.set(750_000_000L);
            keyboard.release(MIDDLE_C);
        });

        int exitCode = run("monitor", "virtual:keyboard");

        assertThat(exitCode).isZero();
        assertThat(out.toString().lines()).containsExactly(
                "Monitoring virtual:keyboard. Press keys on the piano, Ctrl+C to stop.",
                "    0.500 s  NOTE ON   C4   vel  80",
                "    0.750 s  NOTE OFF  C4");
    }

    @Test
    void monitorStopsListeningOnceStopped() {
        run("monitor", "virtual:keyboard");

        keyboard.press(MIDDLE_C, Velocity.of(80));

        assertThat(out.toString().lines()).hasSize(1);
    }

    @Test
    void reportsUnknownDeviceWithoutStackTrace() {
        int exitCode = run("monitor", "jdk:Missing");

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString())
                .contains("MIDI device unavailable: jdk:Missing", "pp devices")
                .doesNotContain("Exception", "\tat ");
    }

    @Test
    void monitorRequiresADevice() {
        int exitCode = run("monitor");

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("Missing required parameter");
    }

    @Test
    void requiresASubcommand() {
        int exitCode = run();

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("Missing required subcommand", "devices", "monitor");
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
        assertThat(out.toString()).contains("Usage: pp", "devices", "monitor");
    }
}
