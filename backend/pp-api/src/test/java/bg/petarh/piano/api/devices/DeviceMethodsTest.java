package bg.petarh.piano.api.devices;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.settings.InMemorySettings;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceMethodsTest {

    private final RpcServer server = new RpcServer();

    DeviceMethodsTest() {
        PianoApi.create(server, VirtualKeyboard.withClock(() -> 0L).port(), SilentOutput.create().port(),
                () -> 0L, (method, params) -> { }, new InMemorySettings());
    }

    @Test
    void listsInputsAndOutputs() {
        assertThat(server.handle("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"devices.list\"}")).contains(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{"
                        + "\"inputs\":[{\"id\":\"virtual:keyboard\",\"name\":\"Virtual keyboard\","
                        + "\"description\":\"" + VirtualKeyboard.withClock(() -> 0L).device().description() + "\"}],"
                        + "\"outputs\":[{\"id\":\"virtual:silent\",\"name\":\"Silent output\","
                        + "\"description\":\"Plays nothing, records every sent event\"}]}}");
    }
}
