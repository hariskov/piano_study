package bg.petarh.piano.api.settings;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class SettingsApiTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final InMemorySettings store = new InMemorySettings();
    private final RpcServer server = new RpcServer();
    private final List<JsonNode> events = new CopyOnWriteArrayList<>();
    private final PianoApi api = PianoApi.create(server, VirtualKeyboard.withClock(System::nanoTime).port(),
            SilentOutput.create().port(), System::nanoTime,
            (method, params) -> events.add(JSON.readTree(server.notification(method, params))), store);

    @AfterEach
    void closeApi() {
        api.close();
    }

    @Test
    void startsWithDefaults() {
        assertThat(call("settings.get", "{}").get("result").toString())
                .isEqualTo("{\"keyboards\":{},\"tempos\":{},\"latencyOffsetMs\":0}");
    }

    @Test
    void updatesAndSavesOnlyWhatIsGiven() {
        call("settings.update", "{\"input\":\"jdk:Piano\",\"keyboards\":{\"jdk:Piano\":\"F1-F7\"}}");
        JsonNode result = call("settings.update", "{\"latencyOffsetMs\":20,\"tempos\":{\"wait\":70}}").get("result");

        assertThat(result.toString()).isEqualTo("{\"input\":\"jdk:Piano\",\"keyboards\":{\"jdk:Piano\":\"F1-F7\"},"
                + "\"tempos\":{\"wait\":70},\"latencyOffsetMs\":20}");
        assertThat(store.saved().keyboardFor("jdk:Piano")).contains(KeyboardRange.parse("F1-F7"));
    }

    @Test
    void rejectsValuesItCannotUse() {
        assertThat(call("settings.update", "{\"keyboards\":{\"jdk:Piano\":\"nonsense\"}}").at("/error/code").asInt())
                .isEqualTo(-32602);
        assertThat(call("settings.update", "{\"latencyOffsetMs\":9999}").at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(call("settings.update", "{\"tempos\":{\"wait\":500}}").at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(store.saves()).isZero();
    }

    @Test
    void sessionRemembersTheInputAndTempo() {
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"mode\":\"tempo\",\"tempo\":75}");

        assertThat(store.saved().input()).contains("virtual:keyboard");
        assertThat(store.saved().tempoFor("tempo")).contains(75);
    }

    @Test
    void sessionUsesTheSavedTempoForItsMode() {
        call("settings.update", "{\"tempos\":{\"wait\":66}}");

        JsonNode started = call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\"}");

        assertThat(started.at("/result/tempo").asInt()).isEqualTo(66);
    }

    @Test
    void sessionKeepsToTheSavedKeyboardOfItsInput() {
        call("settings.update", "{\"keyboards\":{\"virtual:keyboard\":\"C4-C5\"}}");
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"level\":2,\"notes\":20,"
                + "\"countIn\":0,\"seed\":1}");

        JsonNode target = awaitEvent("session.target");

        assertThat(target.at("/params/notes/0/midi").asInt()).isBetween(60, 72);
    }

    @Test
    void changingTheTempoIsRemembered() {
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"notes\":0,\"countIn\":0}");
        awaitEvent("session.state");

        call("session.changeTempo", "{\"bpm\":110}");

        assertThat(store.saved().tempoFor("wait")).contains(110);
    }

    private JsonNode call(String method, String params) {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}";
        return JSON.readTree(server.handle(request).orElseThrow());
    }

    private JsonNode awaitEvent(String method) {
        while (true) {
            for (JsonNode event : events) {
                if (event.get("method").asString().equals(method)) {
                    return event;
                }
            }
            Thread.onSpinWait();
        }
    }
}
