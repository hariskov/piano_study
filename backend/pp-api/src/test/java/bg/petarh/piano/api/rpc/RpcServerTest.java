package bg.petarh.piano.api.rpc;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RpcServerTest {

    record Greeting(String name) {
    }

    record Reply(String text, int length) {
    }

    private final RpcServer server = new RpcServer();

    @Test
    void answersARequestWithTheSameId() {
        server.register("greet", Greeting.class, Object.class, greeting -> new Reply("Hello " + greeting.name(), greeting.name().length()));

        assertThat(server.handle("{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"greet\",\"params\":{\"name\":\"Ana\"}}"))
                .contains("{\"jsonrpc\":\"2.0\",\"id\":7,\"result\":{\"text\":\"Hello Ana\",\"length\":3}}");
    }

    @Test
    void keepsStringIds() {
        server.register("ping", Void.class, Object.class, ignored -> "pong");

        assertThat(server.handle("{\"jsonrpc\":\"2.0\",\"id\":\"a1\",\"method\":\"ping\"}"))
                .contains("{\"jsonrpc\":\"2.0\",\"id\":\"a1\",\"result\":\"pong\"}");
    }

    @Test
    void answersNullForHandlersWithoutResult() {
        server.register("stop", Void.class, Object.class, ignored -> null);

        assertThat(server.handle("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"stop\"}"))
                .contains("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":null}");
    }

    @Test
    void runsNotificationsWithoutAnswering() {
        List<String> seen = new ArrayList<>();
        server.register("log", Greeting.class, Object.class, greeting -> {
            seen.add(greeting.name());
            return null;
        });

        assertThat(server.handle("{\"jsonrpc\":\"2.0\",\"method\":\"log\",\"params\":{\"name\":\"x\"}}")).isEmpty();
        assertThat(seen).containsExactly("x");
    }

    @Test
    void reportsUnknownMethods() {
        assertThat(server.handle("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"dance\"}")).contains(
                "{\"jsonrpc\":\"2.0\",\"id\":3,\"error\":{\"code\":-32601,\"message\":\"Method not found: dance\"}}");
    }

    @Test
    void reportsLinesThatAreNotJson() {
        assertThat(server.handle("{oops")).hasValueSatisfying(response -> assertThat(response)
                .startsWith("{\"jsonrpc\":\"2.0\",\"id\":null,\"error\":{\"code\":-32700,\"message\":\"Parse error"));
    }

    @Test
    void reportsRequestsWithoutAMethod() {
        assertThat(server.handle("{\"jsonrpc\":\"2.0\",\"id\":4}")).contains(
                "{\"jsonrpc\":\"2.0\",\"id\":4,\"error\":{\"code\":-32600,\"message\":\"Invalid request: no method\"}}");
        assertThat(server.handle("[1,2]")).hasValueSatisfying(response -> assertThat(response).contains("-32600"));
    }

    @Test
    void reportsParamsOfTheWrongShape() {
        server.register("greet", Greeting.class, Object.class, greeting -> greeting.name());

        assertThat(server.handle("{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"greet\",\"params\":{\"name\":[1]}}"))
                .hasValueSatisfying(response -> assertThat(response).contains("\"id\":5", "\"code\":-32602"));
    }

    @Test
    void reportsRejectedValuesAsInvalidParams() {
        server.register("greet", Greeting.class, Object.class, greeting -> {
            throw new IllegalArgumentException("Name too short: " + greeting.name());
        });

        assertThat(server.handle("{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"greet\",\"params\":{\"name\":\"A\"}}")).contains(
                "{\"jsonrpc\":\"2.0\",\"id\":6,\"error\":{\"code\":-32602,\"message\":\"Name too short: A\"}}");
    }

    @Test
    void reportsErrorsRaisedByHandlers() {
        server.register("start", Void.class, Object.class, ignored -> {
            throw new RpcException(RpcException.SESSION_ERROR, "A session is already running");
        });

        assertThat(server.handle("{\"jsonrpc\":\"2.0\",\"id\":8,\"method\":\"start\"}")).contains(
                "{\"jsonrpc\":\"2.0\",\"id\":8,\"error\":{\"code\":-32001,\"message\":\"A session is already running\"}}");
    }

    @Test
    void reportsUnexpectedFailuresAsInternalErrors() {
        server.register("crash", Void.class, Object.class, ignored -> {
            throw new IllegalStateException("boom");
        });

        assertThat(server.handle("{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"crash\"}")).contains(
                "{\"jsonrpc\":\"2.0\",\"id\":9,\"error\":{\"code\":-32603,\"message\":\"Internal error: boom\"}}");
    }

    @Test
    void writesEventsAsNotifications() {
        assertThat(server.notification("eval", new Reply("ok", 2)))
                .isEqualTo("{\"jsonrpc\":\"2.0\",\"method\":\"eval\",\"params\":{\"text\":\"ok\",\"length\":2}}");
    }
}
