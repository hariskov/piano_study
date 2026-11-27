package bg.petarh.piano.api.session;

import bg.petarh.piano.api.PianoApi;
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class SessionAdjustTest {

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
    void changesTheHandFromTheNextTarget() {
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"notes\":0,\"countIn\":0,"
                + "\"seed\":2}");
        JsonNode first = await("session.target", 1);

        assertThat(call("session.adjust", "{\"hands\":\"left\"}").get("result").isNull()).isTrue();
        play(first);

        JsonNode next = await("session.target", 2);
        assertThat(next.at("/params/notes/0/hand").asString()).isEqualTo("LEFT");
        assertThat(next.at("/params/notes/0/midi").asInt()).isLessThan(61);
    }

    @Test
    void changesTheChordTypesFromTheNextTarget() {
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"chords\",\"notes\":0,\"countIn\":0,"
                + "\"chordTypes\":[\"maj\"],\"seed\":4}");
        JsonNode first = await("session.target", 1);
        assertThat(first.at("/params/notes")).hasSize(3);

        call("session.adjust", "{\"chordTypes\":[\"dom7\"]}");
        play(first);

        assertThat(await("session.target", 2).at("/params/notes")).hasSize(4);
    }

    @Test
    void rejectsSettingsThatCannotBePlayedAndKeepsTheOldOnes() {
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"notes\":0,\"countIn\":0}");
        await("session.target", 1);

        assertThat(call("session.adjust", "{\"level\":9}").at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(call("session.adjust", "{\"hands\":\"feet\"}").at("/error/code").asInt()).isEqualTo(-32602);
    }

    @Test
    void needsARunningSession() {
        assertThat(call("session.adjust", "{\"level\":2}").at("/error/code").asInt()).isEqualTo(-32001);
    }

    private void play(JsonNode target) {
        List<Pitch> pitches = target.at("/params/notes").valueStream()
                .map(note -> Pitch.parse(note.get("pitch").asString())).toList();
        pitches.forEach(pitch -> keyboard.press(pitch, Velocity.of(64)));
        pitches.forEach(keyboard::release);
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
