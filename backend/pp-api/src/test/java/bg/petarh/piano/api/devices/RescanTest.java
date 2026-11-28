package bg.petarh.piano.api.devices;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.settings.InMemorySettings;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class RescanTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SwitchableInputs inputs = new SwitchableInputs(VirtualKeyboard.withClock(() -> 0L).port());
    private final RpcServer server = new RpcServer();
    private final List<String> events = new CopyOnWriteArrayList<>();
    private final PianoApi api = PianoApi.create(server, inputs, SilentOutput.create().port(), () -> 0L,
            (method, params) -> events.add(method), new InMemorySettings());

    @AfterEach
    void closeApi() {
        api.close();
    }

    @Test
    void looksForDevicesStraightAway() {
        inputs.unplug();

        JsonNode result = call("devices.rescan").get("result");

        assertThat(result.get("inputs")).isEmpty();
        assertThat(events).containsExactly("devices.changed");

        inputs.plugIn();
        assertThat(call("devices.rescan").at("/result/inputs/0/id").asString()).isEqualTo("virtual:keyboard");
    }

    @Test
    void answersEvenWhenNothingChanged() {
        assertThat(call("devices.rescan").at("/result/inputs")).hasSize(1);
        assertThat(events).isEmpty();
    }

    private JsonNode call(String method) {
        return JSON.readTree(server.handle("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\"}").orElseThrow());
    }
}
