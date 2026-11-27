package bg.petarh.piano.app.cli;

import bg.petarh.piano.midi.output.OutputEvent;
import bg.petarh.piano.midi.value.MidiChannel;
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
    void echoPlaysEverythingPressedOnTheOutput() {
        lifetime.whileOpen(() -> {
            now.set(100_000_000L);
            keyboard.press(MIDDLE_C, Velocity.of(90));
            keyboard.sustain(true);
            keyboard.release(MIDDLE_C);
        });

        int exitCode = run("echo", "virtual:keyboard", "virtual:silent");

        MidiChannel first = MidiChannel.of(0);
        assertThat(exitCode).isZero();
        assertThat(output.sentEvents()).containsExactly(
                new OutputEvent.NoteOn(MIDDLE_C, Velocity.of(90), first),
                new OutputEvent.SustainPedal(true, first),
                new OutputEvent.NoteOff(MIDDLE_C, first));
        assertThat(out.toString().lines()).containsExactly(
                "Echoing virtual:keyboard to virtual:silent. Play the piano, Ctrl+C to stop.",
                "    0.100 s  NOTE ON   C4   vel  90",
                "    0.100 s  PEDAL     down",
                "    0.100 s  NOTE OFF  C4");
    }

    @Test
    void echoStopsAfterLifetimeEnds() {
        run("echo", "virtual:keyboard", "virtual:silent");

        keyboard.press(MIDDLE_C, Velocity.of(90));

        assertThat(output.sentEvents()).isEmpty();
    }

    @Test
    void echoReportsUnknownOutput() {
        int exitCode = run("echo", "virtual:keyboard", "jdk:Missing");

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("MIDI device unavailable: jdk:Missing");
    }

    @Test
    void echoReportsUnknownInput() {
        int exitCode = run("echo", "jdk:Missing", "virtual:silent");

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("MIDI device unavailable: jdk:Missing");
    }

    @Test
    void detectRangeGuidesThroughLowestAndHighestKey() {
        lifetime.whileOpen(() -> {
            keyboard.press(Pitch.parse("A0"), Velocity.of(64));
            keyboard.press(Pitch.parse("A0"), Velocity.of(64));
            keyboard.press(Pitch.parse("C8"), Velocity.of(64));
        });

        int exitCode = run("detect-range", "virtual:keyboard");

        assertThat(exitCode).isZero();
        assertThat(out.toString().lines()).containsExactly(
                "Press the lowest key on your keyboard.",
                "Lowest key: A0. Now press the highest key.",
                "Detected range: A0-C8, 88 keys (matches the 88-key preset).");
    }

    @Test
    void detectRangeReportsCustomRanges() {
        lifetime.whileOpen(() -> {
            keyboard.press(Pitch.parse("F7"), Velocity.of(64));
            keyboard.press(Pitch.parse("F1"), Velocity.of(64));
        });

        run("detect-range", "virtual:keyboard");

        assertThat(out.toString()).contains("Detected range: F1-F7, 73 keys (custom range).");
    }

    @Test
    void detectRangeReportsUnknownDevice() {
        int exitCode = run("detect-range", "jdk:Missing");

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("MIDI device unavailable: jdk:Missing");
    }

    @Test
    void requiresASubcommand() {
        int exitCode = run();

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("Missing required subcommand", "devices", "monitor", "echo", "detect-range");
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
        assertThat(out.toString()).contains("Usage: pp", "devices", "monitor", "echo");
    }
}
