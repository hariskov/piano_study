package bg.petarh.piano.api.contract;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.settings.InMemorySettings;
import bg.petarh.piano.library.songs.SongLibrary;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProtocolFileTest {

    @TempDir
    Path folder;


    private static final Path PROTOCOL = Path.of("../../frontend/src/api/protocol.ts");

    @Test
    void frontendProtocolMatchesTheJavaApi() throws Exception {
        RpcServer server = new RpcServer();
        PianoApi api = PianoApi.create(server, VirtualKeyboard.withClock(() -> 0L).port(),
                SilentOutput.create().port(), () -> 0L, (method, params) -> { }, new InMemorySettings(),
                VirtualKeyboard.withClock(() -> 0L), new SongLibrary(folder, () -> Instant.EPOCH));
        try {
            String generated = TypeScriptContract.generate(server.methods(), PianoApi.EVENTS);

            if (Boolean.getBoolean("protocol.write")) {
                Files.createDirectories(PROTOCOL.getParent());
                Files.writeString(PROTOCOL, generated);
            }
            assertThat(PROTOCOL).as("frontend/src/api/protocol.ts is out of date; "
                    + "run ./mvnw -pl backend/pp-api -am test -Dprotocol.write=true").hasContent(generated);
        } finally {
            api.close();
        }
    }
}
