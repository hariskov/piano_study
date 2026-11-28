package bg.petarh.piano.app.cli;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.stdio.StdioTransport;
import bg.petarh.piano.library.settings.JsonSettingsFile;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.library.settings.SettingsLocation;
import bg.petarh.piano.library.progress.ProgressLog;
import bg.petarh.piano.library.songs.SongLibrary;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
        name = "api",
        mixinStandardHelpOptions = true,
        description = "Serve the JSON-RPC API on standard input and output, one message per line (used by the UI).")
final class ApiCommand implements Callable<Integer> {

    private final CliEnvironment environment;

    @Option(names = "--settings", paramLabel = "FILE",
            description = "Settings file (default: ~/.config/piano-player/settings.json, or under $XDG_CONFIG_HOME).")
    private Optional<Path> settingsFile;

    @Option(names = "--library", paramLabel = "DIR",
            description = "Song library folder (default: ~/.local/share/piano-player/musicFiles). "
                    + "Practice history is kept next to it, in progress/.")
    private Optional<Path> library;

    ApiCommand(CliEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Integer call() {
        RpcServer server = new RpcServer();
        StdioTransport transport = new StdioTransport(server);
        VirtualKeyboard keyboard = VirtualKeyboard.withClock(environment.clock());
        MidiInputPort inputs = MidiInputPort.combining(List.of(environment.inputs(), keyboard.port()));
        Path libraryFolder = library.orElseGet(SongLibrary::defaultFolder);
        try (PianoApi api = PianoApi.create(server, inputs, environment.outputs(), environment.clock(),
                transport.events(), new JsonSettingsFile(settingsFile.orElseGet(SettingsLocation::defaultSettingsFile)),
                keyboard, new SongLibrary(libraryFolder, Instant::now),
                new ProgressLog(libraryFolder.resolveSibling("progress")), InstantSource.system())) {
            transport.onEndOfInput(api::close);
            transport.run(environment.stdin(), environment.stdout());
        }
        return 0;
    }
}
