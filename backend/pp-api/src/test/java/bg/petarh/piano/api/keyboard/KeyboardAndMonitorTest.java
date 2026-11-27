package bg.petarh.piano.api.keyboard;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.settings.InMemorySettings;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class KeyboardAndMonitorTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final VirtualKeyboard keyboard = VirtualKeyboard.withClock(System::nanoTime);
    private final RpcServer server = new RpcServer();
    private final List<JsonNode> events = new CopyOnWriteArrayList<>();
    private final PianoApi api = PianoApi.create(server, keyboard.port(), SilentOutput.create().port(),
            System::nanoTime, (method, params) -> events.add(JSON.readTree(server.notification(method, params))),
            new InMemorySettings(), keyboard);

    @AfterEach
    void closeApi() {
        api.close();
    }

    @Test
    void monitorsAnInputWithoutASession() {
        call("devices.monitor", "{\"input\":\"virtual:keyboard\"}");

        call("keyboard.press", "{\"pitch\":\"C4\",\"velocity\":90}");
        call("keyboard.release", "{\"pitch\":\"C4\"}");

        assertThat(inputEvents()).extracting(event -> event.get("type").asString() + " " + event.get("pitch").asString())
                .containsExactly("NOTE_ON C4", "NOTE_OFF C4");
        assertThat(inputEvents().getFirst().get("velocity").asInt()).isEqualTo(90);
    }

    @Test
    void pressesWithAMediumVelocityByDefault() {
        call("devices.monitor", "{\"input\":\"virtual:keyboard\"}");

        call("keyboard.press", "{\"pitch\":\"A4\"}");

        assertThat(inputEvents().getFirst().get("velocity").asInt()).isEqualTo(80);
    }

    @Test
    void stopsMonitoring() {
        call("devices.monitor", "{\"input\":\"virtual:keyboard\"}");
        call("devices.stopMonitor", "{}");

        call("keyboard.press", "{\"pitch\":\"C4\"}");

        assertThat(inputEvents()).isEmpty();
    }

    @Test
    void aSessionTakesOverTheMonitoredInput() {
        call("devices.monitor", "{\"input\":\"virtual:keyboard\"}");
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"notes\":0,\"countIn\":0}");

        call("keyboard.press", "{\"pitch\":\"C4\"}");

        assertThat(inputEvents()).hasSize(1);
    }

    @Test
    void rejectsUnknownPitchesAndDevices() {
        assertThat(call("keyboard.press", "{\"pitch\":\"H4\"}").at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(call("devices.monitor", "{\"input\":\"jdk:Missing\"}").at("/error/code").asInt()).isEqualTo(-32002);
    }

    private JsonNode call(String method, String params) {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}";
        return JSON.readTree(server.handle(request).orElseThrow());
    }

    private List<JsonNode> inputEvents() {
        return events.stream().filter(event -> event.get("method").asString().equals("input"))
                .map(event -> event.get("params")).toList();
    }
}
