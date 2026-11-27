package bg.petarh.piano.app.cli;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.stdio.StdioTransport;
import bg.petarh.piano.library.settings.JsonSettingsFile;
import bg.petarh.piano.library.settings.SettingsLocation;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
        name = "api",
        mixinStandardHelpOptions = true,
        description = "Serve the JSON-RPC API on standard input and output, one message per line (used by the UI).")
final class ApiCommand implements Callable<Integer> {

    private static final Duration DEVICE_CHECK_INTERVAL = Duration.ofSeconds(1);

    private final CliEnvironment environment;

    @Option(names = "--settings", paramLabel = "FILE",
            description = "Settings file (default: ~/.config/piano-player/settings.json, or under $XDG_CONFIG_HOME).")
    private Optional<Path> settingsFile;

    ApiCommand(CliEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Integer call() {
        RpcServer server = new RpcServer();
        StdioTransport transport = new StdioTransport(server);
        try (PianoApi api = PianoApi.create(server, environment.inputs(), environment.outputs(), environment.clock(),
                transport.events(), new JsonSettingsFile(settingsFile.orElseGet(SettingsLocation::defaultSettingsFile)))) {
            transport.onEndOfInput(api::close);
            api.startWatchingDevices(DEVICE_CHECK_INTERVAL);
            transport.run(environment.stdin(), environment.stdout());
        }
        return 0;
    }
}
