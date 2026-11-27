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
class SessionApiTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final VirtualKeyboard keyboard = VirtualKeyboard.withClock(System::nanoTime);
    private final SilentOutput silent = SilentOutput.create();
    private final RpcServer server = new RpcServer();
    private final List<JsonNode> events = new CopyOnWriteArrayList<>();
    private final PianoApi api = PianoApi.create(server, keyboard.port(), silent.port(), System::nanoTime,
            (method, params) -> events.add(JSON.readTree(server.notification(method, params))),
            new InMemorySettings());

    @AfterEach
    void closeApi() {
        api.close();
    }

    @Test
    void playsARandomSessionFromStartToSummary() {
        JsonNode started = call("session.start", """
                {"input":"virtual:keyboard","exercise":"random","notes":3,"seed":7,"countIn":0}""");

        assertThat(started.at("/result/seed").asLong()).isEqualTo(7);
        for (int i = 1; i <= 3; i++) {
            JsonNode target = awaitEvent("session.target", i);
            Pitch pitch = Pitch.parse(target.at("/params/notes/0/pitch").asString());
            keyboard.press(pitch, Velocity.of(64));
            keyboard.release(pitch);
        }

        JsonNode summary = awaitEvent("session.summary", 1).get("params");
        assertThat(summary.get("completedTargets").asInt()).isEqualTo(3);
        assertThat(summary.get("accuracyPercent").asInt()).isEqualTo(100);
        assertThat(summary.get("timed").asBoolean()).isFalse();
        assertThat(methods()).contains("session.state", "session.target", "eval", "input");
        assertThat(events).filteredOn(event -> event.get("method").asString().equals("eval"))
                .extracting(event -> event.at("/params/result").asString())
                .contains("CORRECT", "TARGET_COMPLETED");
    }

    @Test
    void namesChordTargets() {
        call("session.start", """
                {"input":"virtual:keyboard","exercise":"chords","chordTypes":["maj"],"inversions":[0],
                 "notes":1,"seed":3,"countIn":0}""");

        JsonNode target = awaitEvent("session.target", 1).get("params");

        assertThat(target.get("name").asString()).matches("[A-G][#b]?");
        assertThat(target.get("notes")).hasSize(3);
        assertThat(target.at("/notes/0/hand").asString()).isEqualTo("RIGHT");
    }

    @Test
    void reportsTimedResultsAndBeatsInTempoMode() {
        call("session.start", """
                {"input":"virtual:keyboard","exercise":"random","mode":"tempo","tempo":240,"notes":1,"countIn":1,
                 "metronome":"virtual:silent"}""");

        JsonNode summary = awaitEvent("session.summary", 1).get("params");

        assertThat(summary.get("timed").asBoolean()).isTrue();
        assertThat(summary.get("missed").asInt()).isEqualTo(1);
        assertThat(events).filteredOn(event -> event.get("method").asString().equals("beat"))
                .first().satisfies(beat -> assertThat(beat.at("/params/accent").asBoolean()).isTrue());
        assertThat(silent.sentEvents()).isNotEmpty();
    }

    @Test
    void controlsARunningSession() {
        call("session.start", """
                {"input":"virtual:keyboard","exercise":"random","notes":0,"countIn":0}""");
        awaitEvent("session.state", 1);

        assertThat(call("session.pause", "{}").get("result").isNull()).isTrue();
        assertThat(call("session.changeTempo", "{\"bpm\":60}").get("result").isNull()).isTrue();
        assertThat(call("session.resume", "{}").get("result").isNull()).isTrue();
        call("session.stop", "{}");

        assertThat(awaitEvent("session.summary", 1)).isNotNull();
        assertThat(events).filteredOn(event -> event.get("method").asString().equals("session.state"))
                .extracting(event -> event.at("/params/state").asString())
                .containsSubsequence("RUNNING", "PAUSED", "RUNNING", "FINISHED");
    }

    @Test
    void allowsANewSessionOnceTheLastHasFinished() {
        call("session.start", """
                {"input":"virtual:keyboard","exercise":"random","notes":0,"countIn":0}""");
        call("session.stop", "{}");
        awaitEvent("session.summary", 1);

        assertThat(call("session.start", """
                {"input":"virtual:keyboard","exercise":"random","notes":0,"countIn":0}""").has("result")).isTrue();
    }

    @Test
    void rejectsASecondSessionWhileOneIsRunning() {
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"notes\":0}");

        JsonNode second = call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\"}");

        assertThat(second.at("/error/code").asInt()).isEqualTo(-32001);
    }

    @Test
    void rejectsControlsWithoutASession() {
        assertThat(call("session.pause", "{}").at("/error/code").asInt()).isEqualTo(-32001);
        assertThat(call("session.stop", "{}").at("/error/code").asInt()).isEqualTo(-32001);
    }

    @Test
    void rejectsUnknownDevices() {
        JsonNode response = call("session.start", "{\"input\":\"jdk:Missing\",\"exercise\":\"random\"}");

        assertThat(response.at("/error/code").asInt()).isEqualTo(-32002);
        assertThat(response.at("/error/message").asString()).contains("jdk:Missing");
    }

    @Test
    void rejectsSettingsThatCannotBePlayed() {
        assertThat(call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"tempo\":500}")
                .at("/error/message").asString()).contains("Tempo out of range");
        assertThat(call("session.start", "{\"exercise\":\"random\"}").at("/error/message").asString())
                .contains("input is required");
        assertThat(call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"dance\"}")
                .at("/error/code").asInt()).isEqualTo(-32602);
        assertThat(call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"key\":\"H\"}")
                .at("/error/code").asInt()).isEqualTo(-32602);
    }

    private JsonNode call(String method, String params) {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}";
        return JSON.readTree(server.handle(request).orElseThrow());
    }

    private JsonNode awaitEvent(String method, int occurrence) {
        while (true) {
            List<JsonNode> matching = events.stream()
                    .filter(event -> event.get("method").asString().equals(method)).toList();
            if (matching.size() >= occurrence) {
                return matching.get(occurrence - 1);
            }
            Thread.onSpinWait();
        }
    }

    private List<String> methods() {
        return events.stream().map(event -> event.get("method").asString()).distinct().toList();
    }
}
