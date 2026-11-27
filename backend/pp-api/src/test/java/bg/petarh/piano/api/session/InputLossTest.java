package bg.petarh.piano.api.session;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.devices.SwitchableInputs;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.settings.InMemorySettings;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class InputLossTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final VirtualKeyboard keyboard = VirtualKeyboard.withClock(System::nanoTime);
    private final SwitchableInputs inputs = new SwitchableInputs(keyboard.port());
    private final RpcServer server = new RpcServer();
    private final List<JsonNode> events = new CopyOnWriteArrayList<>();
    private final PianoApi api = PianoApi.create(server, inputs, SilentOutput.create().port(), System::nanoTime,
            (method, params) -> events.add(JSON.readTree(server.notification(method, params))), new InMemorySettings());

    @AfterEach
    void closeApi() {
        api.close();
    }

    @Test
    void pausesWhenTheInputIsUnpluggedAndReconnectsWhenItReturns() {
        api.startWatchingDevices(Duration.ofMillis(5));
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"notes\":2,\"countIn\":0,"
                + "\"seed\":5}");
        Pitch first = Pitch.parse(await("session.target", 1).at("/params/notes/0/pitch").asString());

        inputs.unplug();
        assertThat(await("session.inputLost", 1).at("/params/input").asString()).isEqualTo("virtual:keyboard");
        assertThat(await("session.state", 2).at("/params/state").asString()).isEqualTo("PAUSED");

        inputs.plugIn();
        await("session.inputRestored", 1);
        call("session.resume", "{}");
        await("session.state", 3);
        keyboard.press(first, Velocity.of(64));

        assertThat(await("eval", 1).at("/params/result").asString()).isEqualTo("CORRECT");
        assertThat(events).filteredOn(event -> event.get("method").asString().equals("devices.changed")).hasSize(2);
    }

    private JsonNode call(String method, String params) {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}";
        return JSON.readTree(server.handle(request).orElseThrow());
    }

    private JsonNode await(String method, int occurrence) {
        while (true) {
            List<JsonNode> matching = events.stream()
                    .filter(event -> event.get("method").asString().equals(method)).toList();
            if (matching.size() >= occurrence) {
                return matching.get(occurrence - 1);
            }
            Thread.onSpinWait();
        }
    }
}
