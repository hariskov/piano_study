package bg.petarh.piano.app.cli;

import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(5)
class ApiCommandTest {

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

    private int run(String input) {
        CliEnvironment environment = new CliEnvironment(
                VirtualKeyboard.withClock(System::nanoTime).port(), SilentOutput.create().port(), System::nanoTime,
                new ScriptedLifetime(), new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)), stdout);
        return PianoCli.create(environment, new PrintWriter(out, true), new PrintWriter(new StringWriter(), true))
                .execute("api");
    }
}
