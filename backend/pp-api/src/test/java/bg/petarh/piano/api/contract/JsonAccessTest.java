package bg.petarh.piano.api.contract;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.rpc.ApiEvent;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.settings.InMemorySettings;
import bg.petarh.piano.library.songs.SongLibrary;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.module.ModuleDescriptor;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JsonAccessTest {

    @TempDir
    Path folder;


    @Test
    void everyPackageWithApiRecordsIsOpenToJackson() {
        RpcServer server = new RpcServer();
        PianoApi api = PianoApi.create(server, VirtualKeyboard.withClock(() -> 0L).port(),
                SilentOutput.create().port(), () -> 0L, (method, params) -> { }, new InMemorySettings(),
                VirtualKeyboard.withClock(() -> 0L), new SongLibrary(folder, () -> Instant.EPOCH));
        try {
            Set<String> recordPackages = Stream.concat(
                            server.methods().stream().flatMap(method -> Stream.of(method.paramsType(), method.resultType())),
                            PianoApi.EVENTS.stream().map(ApiEvent::paramsType))
                    .filter(Class::isRecord)
                    .map(Class::getPackageName)
                    .collect(Collectors.toSet());
            Set<String> openToJackson = PianoApi.class.getModule().getDescriptor().opens().stream()
                    .filter(opens -> opens.targets().contains("tools.jackson.databind"))
                    .map(ModuleDescriptor.Opens::source)
                    .collect(Collectors.toSet());

            assertThat(openToJackson).containsAll(recordPackages);
        } finally {
            api.close();
        }
    }
}
