package bg.petarh.piano.api.library;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.settings.InMemorySettings;
import bg.petarh.piano.library.songs.SongLibrary;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LibraryApiTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final String MELODY = """
            <?xml version="1.0" encoding="UTF-8"?>
            <score-partwise version="4.0">
              <work><work-title>Simple Melody</work-title></work>
              <identification><creator type="composer">Anon</creator></identification>
              <part-list><score-part id="P1"><part-name>Flute</part-name></score-part></part-list>
              <part id="P1"><measure number="1"><attributes><divisions>1</divisions></attributes>
                <note><pitch><step>C</step><octave>5</octave></pitch><duration>2</duration><voice>1</voice></note>
                <note><pitch><step>G</step><octave>5</octave></pitch><duration>2</duration><voice>1</voice></note>
              </measure></part>
            </score-partwise>
            """;

    @TempDir
    Path folder;

    private final RpcServer server = new RpcServer();
    private PianoApi api;

    @BeforeEach
    void createApi() {
        VirtualKeyboard keyboard = VirtualKeyboard.withClock(() -> 0L);
        api = PianoApi.create(server, keyboard.port(), SilentOutput.create().port(), () -> 0L, (method, params) -> { },
                new InMemorySettings(), keyboard, new SongLibrary(folder.resolve("musicFiles"), () -> Instant.EPOCH));
    }

    @AfterEach
    void closeApi() {
        api.close();
    }

    @Test
    void importsListsOpensAndDeletesSongs() throws IOException {
        Path file = Files.writeString(folder.resolve("melody.musicxml"), MELODY);

        JsonNode imported = call("library.import", "{\"path\":\"" + file + "\"}").get("result");
        assertThat(imported.get("id").asString()).isEqualTo("simple-melody");
        assertThat(imported.get("fileName").asString()).isEqualTo("Simple Melody.musicxml");

        assertThat(call("library.list", "{}").at("/result/songs/0/title").asString()).isEqualTo("Simple Melody");

        JsonNode opened = call("library.open", "{\"id\":\"simple-melody\"}").get("result");
        assertThat(opened.get("composer").asString()).isEqualTo("Anon");
        assertThat(opened.get("timeSignature").asString()).isEqualTo("4/4");
        assertThat(opened.get("key").asString()).isEqualTo("C major");
        assertThat(opened.get("tempo").asInt()).isEqualTo(120);
        assertThat(opened.get("file").asString()).endsWith("musicFiles/xml/Simple Melody.musicxml");
        assertThat(opened.at("/parts/0").toString())
                .isEqualTo("{\"id\":\"P1\",\"name\":\"Flute\",\"staffCount\":1,\"noteCount\":2,\"lowest\":\"C5\",\"highest\":\"G5\"}");

        assertThat(call("library.delete", "{\"id\":\"simple-melody\"}").get("result").isNull()).isTrue();
        assertThat(call("library.list", "{}").at("/result/songs")).isEmpty();
    }

    @Test
    void importsMidiWithChosenOptions() throws IOException {
        Path midi = Files.write(folder.resolve("tune.mid"), midiFile());

        JsonNode imported = call("library.import", "{\"path\":\"" + midi + "\",\"grid\":8,\"triplets\":false,"
                + "\"splitPoint\":\"G4\"}").get("result");

        assertThat(imported.get("format").asString()).isEqualTo("MIDI");
        assertThat(imported.get("midiFile").asString()).isEqualTo("tune.mid");
        assertThat(imported.get("warnings").toString()).contains("estimated");
    }

    @Test
    void explainsFilesItCannotRead() throws IOException {
        Path broken = Files.writeString(folder.resolve("broken.musicxml"), "<score-partwise>");

        JsonNode error = call("library.import", "{\"path\":\"" + broken + "\"}").get("error");

        assertThat(error.get("code").asInt()).isEqualTo(-32003);
        assertThat(error.get("message").asString()).contains("broken.musicxml");
        assertThat(call("library.import", "{\"path\":\"" + folder.resolve("missing.mid") + "\"}").at("/error/code").asInt())
                .isEqualTo(-32003);
        assertThat(call("library.import", "{\"path\":\"" + folder.resolve("song.pdf") + "\"}").at("/error/code").asInt())
                .isEqualTo(-32602);
    }

    @Test
    void givesTheSavedMusicXmlForDrawingTheScore() throws IOException {
        Path file = Files.writeString(folder.resolve("melody.musicxml"), MELODY);
        call("library.import", "{\"path\":\"" + file + "\"}");

        String xml = call("library.musicXml", "{\"id\":\"simple-melody\"}").at("/result/xml").asString();

        assertThat(xml).startsWith("<?xml").contains("<score-partwise").contains("Simple Melody");
        assertThat(call("library.musicXml", "{\"id\":\"nope\"}").at("/error/code").asInt()).isEqualTo(-32004);
    }

    @Test
    void saysWhenASongIsNotThere() {
        assertThat(call("library.open", "{\"id\":\"nothing\"}").at("/error/code").asInt()).isEqualTo(-32004);
        assertThat(call("library.delete", "{\"id\":\"nothing\"}").at("/error/code").asInt()).isEqualTo(-32004);
    }

    private JsonNode call(String method, String params) {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}";
        return JSON.readTree(server.handle(request).orElseThrow());
    }

    private static byte[] midiFile() {
        byte[] track = {0, (byte) 0x90, 64, 0x50, (byte) 0x83, 0x60, (byte) 0x80, 64, 0, 0, (byte) 0xFF, 0x2F, 0};
        byte[] header = {'M', 'T', 'h', 'd', 0, 0, 0, 6, 0, 0, 0, 1, 0x01, (byte) 0xE0,
                'M', 'T', 'r', 'k', 0, 0, 0, (byte) track.length};
        byte[] file = new byte[header.length + track.length];
        System.arraycopy(header, 0, file, 0, header.length);
        System.arraycopy(track, 0, file, header.length, track.length);
        return file;
    }
}
