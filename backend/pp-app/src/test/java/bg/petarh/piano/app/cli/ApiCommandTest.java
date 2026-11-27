package bg.petarh.piano.app.cli;

import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(5)
class ApiCommandTest {

    @TempDir
    Path folder;

    private Path settingsFile;
    private final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    private final StringWriter out = new StringWriter();

    @Test
    void servesJsonRpcOnStandardInputAndOutputUntilTheInputEnds() {
        int exitCode = run("""
                {"jsonrpc":"2.0","id":1,"method":"devices.list"}
                {"jsonrpc":"2.0","id":2,"method":"nope"}
                """);

        assertThat(exitCode).isZero();
        assertThat(stdout.toString(StandardCharsets.UTF_8).lines().toList())
                .hasSize(2)
                .satisfiesExactly(
                        first -> assertThat(first).startsWith("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{\"inputs\":["),
                        second -> assertThat(second).contains("\"id\":2", "-32601"));
        assertThat(out.toString()).isEmpty();
    }

    @Test
    void keepsSettingsInTheGivenFile() {
        run("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"settings.update\",\"params\":{\"input\":\"jdk:Piano\"}}\n");

        assertThat(settingsFile).content().contains("jdk:Piano");
    }

    private int run(String input) {
        settingsFile = folder.resolve("settings.json");
        CliEnvironment environment = new CliEnvironment(
                VirtualKeyboard.withClock(System::nanoTime).port(), SilentOutput.create().port(), System::nanoTime,
                new ScriptedLifetime(), new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), stdout);
        return PianoCli.create(environment, new PrintWriter(out, true), new PrintWriter(new StringWriter(), true))
                .execute("api", "--settings", settingsFile.toString());
    }
}
