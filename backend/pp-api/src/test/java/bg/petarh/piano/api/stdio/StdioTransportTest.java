package bg.petarh.piano.api.stdio;

import bg.petarh.piano.api.rpc.RpcServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(5)
class StdioTransportTest {

    private final RpcServer server = new RpcServer();
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    StdioTransportTest() {
        server.register("ping", Void.class, Object.class, ignored -> "pong");
    }

    @Test
    void answersEachRequestLineAndStopsAtTheEndOfInput() {
        new StdioTransport(server).run(input("""
                {"jsonrpc":"2.0","id":1,"method":"ping"}
                {"jsonrpc":"2.0","id":2,"method":"ping"}
                """), out);

        assertThat(output()).containsExactly(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":\"pong\"}",
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":\"pong\"}");
    }

    @Test
    void skipsBlankLines() {
        new StdioTransport(server).run(input("\n   \n{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"ping\"}\n"), out);

        assertThat(output()).hasSize(1);
    }

    @Test
    void sendsEventsFromOtherThreads() throws Exception {
        PipedOutputStream requests = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream(requests);
        StdioTransport transport = new StdioTransport(server);
        Thread running = Thread.ofPlatform().start(() -> transport.run(in, out));

        Thread.ofPlatform().start(() -> transport.events().emit("beat", 1)).join();
        requests.close();
        running.join();

        assertThat(output()).containsExactly("{\"jsonrpc\":\"2.0\",\"method\":\"beat\",\"params\":1}");
    }

    @Test
    void runsTheShutdownActionAtTheEndOfInput() {
        StringBuilder log = new StringBuilder();
        StdioTransport transport = new StdioTransport(server);
        transport.onEndOfInput(() -> log.append("stopped"));

        transport.run(input(""), out);

        assertThat(log).hasToString("stopped");
    }

    @Test
    void dropsEventsAfterItStopped() {
        StdioTransport transport = new StdioTransport(server);
        transport.run(input(""), out);

        transport.events().emit("beat", 2);

        assertThat(output()).isEmpty();
    }

    private static ByteArrayInputStream input(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
    }

    private java.util.List<String> output() {
        return out.toString(StandardCharsets.UTF_8).lines().toList();
    }
}
