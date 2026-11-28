package bg.petarh.piano.api.session;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.settings.InMemorySettings;
import bg.petarh.piano.library.progress.ProgressLog;
import bg.petarh.piano.library.songs.SongLibrary;
import bg.petarh.piano.midi.output.OutputEvent;
import bg.petarh.piano.midi.value.MidiChannel;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.score.ScoreNote;
import bg.petarh.piano.model.score.Spelling;
import bg.petarh.piano.model.score.Tie;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(10)
class SongSessionTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @TempDir
    Path folder;

    private final VirtualKeyboard keyboard = VirtualKeyboard.withClock(System::nanoTime);
    private final SilentOutput silent = SilentOutput.create();
    private final RpcServer server = new RpcServer();
    private final List<JsonNode> events = new CopyOnWriteArrayList<>();
    private PianoApi api;
    private String songId;

    @BeforeEach
    void createApi() {
        SongLibrary library = new SongLibrary(folder.resolve("musicFiles"), () -> Instant.EPOCH);
        Score song = Score.builder("Duet").length(Fraction.whole(1))
                .tempo(Fraction.ZERO, Tempo.of(100))
                .timeSignature(Fraction.ZERO, TimeSignature.parse("3/4"))
                .part(new Part("P1", "Piano", 2, List.of(
                        note("E5", 0, 1), note("G5", 1, 1), note("C3", 0, 2), note("C8", 2, 1))))
                .part(new Part("P2", "Flute", 1, List.of(note("C6", 0, 1))))
                .build();
        songId = library.add(song, "duet.musicxml", "MusicXML", List.of()).id();
        api = PianoApi.create(server, keyboard.port(), silent.port(), System::nanoTime,
                (method, params) -> events.add(JSON.readTree(server.notification(method, params))),
                new InMemorySettings(), keyboard, library, new ProgressLog(folder.resolve("progress")),
                () -> Instant.parse("2026-11-28T10:00:00Z"));
    }

    @AfterEach
    void closeApi() {
        api.close();
    }

    @Test
    void playsTheChosenPartAndHandOfASongToTheEnd() {
        JsonNode started = call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\""
                + songId + "\",\"part\":\"P1\",\"hands\":\"right\",\"keyboard\":\"61\",\"countIn\":0}").get("result");

        assertThat(started.get("songTempo").asInt()).isEqualTo(100);
        assertThat(started.get("tempo").asInt()).isEqualTo(100);
        assertThat(started.get("sectionStart").asString()).isEqualTo("0");
        assertThat(started.get("passLength").asString()).isEqualTo("1");
        assertThat(started.get("notices").toString()).contains("1 note outside your keyboard was moved by an octave");
        for (int i = 1; i <= 3; i++) {
            JsonNode target = await("session.target", i);
            Pitch pitch = Pitch.parse(target.at("/params/notes/0/pitch").asString());
            keyboard.press(pitch, Velocity.of(64));
            keyboard.release(pitch);
        }
        assertThat(await("session.summary", 1).at("/params/completedTargets").asInt()).isEqualTo(3);
        assertThat(events).filteredOn(event -> event.get("method").asString().equals("session.target"))
                .extracting(event -> event.at("/params/notes/0/pitch").asString())
                .containsExactly("E5", "G5", "C7");
    }

    @Test
    void scalesTheSongsTempo() {
        JsonNode started = call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\""
                + songId + "\",\"tempoPercent\":50,\"countIn\":0}").get("result");

        assertThat(started.get("tempo").asInt()).isEqualTo(50);
        assertThat(started.get("songTempo").asInt()).isEqualTo(100);
    }

    @Test
    void skipsNotesOutsideTheKeyboardWhenAsked() {
        JsonNode started = call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\""
                + songId + "\",\"keyboard\":\"61\",\"outOfRange\":\"skip\",\"countIn\":0}").get("result");

        assertThat(started.get("notices").toString()).contains("1 note outside your keyboard was skipped");
    }

    @Test
    void playsTheOtherPartsWhileYouPlay() {
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\"" + songId
                + "\",\"mode\":\"tempo\",\"tempoPercent\":240,\"countIn\":0,\"playback\":\"virtual:silent\","
                + "\"metronome\":\"virtual:silent\"}");

        await("session.summary", 1);

        assertThat(silent.sentEvents()).filteredOn(event -> event instanceof OutputEvent.NoteOn on
                        && on.channel().equals(MidiChannel.of(1)))
                .extracting(event -> ((OutputEvent.NoteOn) event).pitch().name())
                .contains("C6", "C3").doesNotContain("E5", "G5");
    }

    @Test
    void listenPlaysTheWholeSongAndEndsOnItsOwn() {
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\"" + songId
                + "\",\"mode\":\"listen\",\"tempoPercent\":240,\"countIn\":0,\"playback\":\"virtual:silent\"}");

        assertThat(await("session.summary", 1).at("/params/timed").asBoolean()).isFalse();
        assertThat(silent.sentEvents()).filteredOn(event -> event instanceof OutputEvent.NoteOn)
                .extracting(event -> ((OutputEvent.NoteOn) event).pitch().name())
                .contains("E5", "G5", "C6");
    }

    @Test
    void acceptsPlayAlongAsAMode() {
        assertThat(call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\"" + songId
                + "\",\"mode\":\"play-along\",\"countIn\":0}").has("result")).isTrue();
    }

    @Test
    void repeatsAChosenSectionAndScoresEachPass() {
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\"" + songId
                + "\",\"fromBar\":1,\"toBar\":1,\"repeats\":2,\"keyboard\":\"61\",\"countIn\":0}");

        for (int i = 1; i <= 6; i++) {
            JsonNode target = await("session.target", i);
            Pitch pitch = Pitch.parse(target.at("/params/notes/0/pitch").asString());
            if (i == 4) {
                keyboard.press(Pitch.parse("A0"), Velocity.of(64));
                keyboard.release(Pitch.parse("A0"));
            }
            keyboard.press(pitch, Velocity.of(64));
            keyboard.release(pitch);
        }

        JsonNode summary = await("session.summary", 1).get("params");
        assertThat(summary.get("completedTargets").asInt()).isEqualTo(6);
        assertThat(summary.get("passes").toString()).isEqualTo("[100,75]");
    }

    @Test
    void saysWhereTheSectionStartsInTheSong() {
        JsonNode started = call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\""
                + songId + "\",\"fromBar\":1,\"toBar\":1,\"repeats\":3,\"countIn\":0}").get("result");

        assertThat(started.get("sectionStart").asString()).isEqualTo("0");
        assertThat(started.get("passLength").asString()).isEqualTo("3/4");
    }

    @Test
    void savesEachFinishedSessionWithItsMistakesPerBar() {
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\"" + songId
                + "\",\"keyboard\":\"61\",\"tempoPercent\":80,\"countIn\":0}");
        for (int i = 1; i <= 3; i++) {
            Pitch pitch = Pitch.parse(await("session.target", i).at("/params/notes/0/pitch").asString());
            if (i == 2) {
                keyboard.press(Pitch.parse("A0"), Velocity.of(64));
                keyboard.release(Pitch.parse("A0"));
            }
            keyboard.press(pitch, Velocity.of(64));
            keyboard.release(pitch);
        }
        await("session.summary", 1);

        JsonNode sessions = call("progress.history", "{\"songId\":\"" + songId + "\"}").at("/result/sessions");

        assertThat(sessions).hasSize(1);
        JsonNode saved = sessions.get(0);
        assertThat(saved.get("finishedAt").asString()).isEqualTo("2026-11-28T10:00:00Z");
        assertThat(saved.get("exercise").asString()).isEqualTo("song");
        assertThat(saved.get("part").asString()).isEqualTo("P1");
        assertThat(saved.get("tempoPercent").asInt()).isEqualTo(80);
        assertThat(saved.get("accuracyPercent").asInt()).isEqualTo(75);
        assertThat(saved.get("mistakesByBar").toString()).isEqualTo("{\"1\":1}");
    }

    @Test
    void doesNotSaveListeningAndKeepsRandomPracticeApart() {
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\"" + songId
                + "\",\"mode\":\"listen\",\"tempoPercent\":240,\"countIn\":0}");
        await("session.summary", 1);
        int targetsSoFar = (int) events.stream().filter(event -> event.get("method").asString().equals("session.target"))
                .count();
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"notes\":1,\"countIn\":0}");
        Pitch pitch = Pitch.parse(await("session.target", targetsSoFar + 1).at("/params/notes/0/pitch").asString());
        keyboard.press(pitch, Velocity.of(64));
        keyboard.release(pitch);
        await("session.summary", 2);

        assertThat(call("progress.history", "{\"songId\":\"" + songId + "\"}").at("/result/sessions")).isEmpty();
        assertThat(call("progress.history", "{\"exercise\":\"random\"}").at("/result/sessions")).hasSize(1);
        assertThat(call("progress.history", "{}").at("/error/code").asInt()).isEqualTo(-32602);
    }

    @Test
    void refusesBarsOutsideTheSong() {
        assertThat(call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\""
                + songId + "\",\"fromBar\":1,\"toBar\":5}").at("/error/message").asString()).contains("2 bars");
    }

    @Test
    void refusesUnknownSongsPartsAndAdjusting() {
        assertThat(call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\"nope\"}")
                .at("/error/code").asInt()).isEqualTo(-32004);
        assertThat(call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\"" + songId
                + "\",\"part\":\"P2\",\"hands\":\"left\"}").at("/error/message").asString()).contains("Flute");

        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"song\",\"songId\":\"" + songId
                + "\",\"countIn\":0}");
        assertThat(call("session.adjust", "{\"hands\":\"left\"}").at("/error/code").asInt()).isEqualTo(-32602);
    }

    private JsonNode call(String method, String params) {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}";
        return JSON.readTree(server.handle(request).orElseThrow());
    }

    private JsonNode await(String method, int occurrence) {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (true) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("No " + method + " #" + occurrence + " in time; events: " + events);
            }
            List<JsonNode> matching = events.stream()
                    .filter(event -> event.get("method").asString().equals(method)).toList();
            if (matching.size() >= occurrence) {
                return matching.get(occurrence - 1);
            }
            Thread.onSpinWait();
        }
    }

    private static ScoreNote note(String pitch, int quarter, int staff) {
        return new ScoreNote(Spelling.of(Pitch.parse(pitch), false), Fraction.of(quarter, 4), Fraction.of(1, 4), staff, 1,
                Tie.NONE);
    }
}
