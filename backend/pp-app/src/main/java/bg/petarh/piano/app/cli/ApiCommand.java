package bg.petarh.piano.app.cli;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.stdio.StdioTransport;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

@Command(
        name = "api",
        mixinStandardHelpOptions = true,
        description = "Serve the JSON-RPC API on standard input and output, one message per line (used by the UI).")
final class ApiCommand implements Callable<Integer> {

    private final CliEnvironment environment;

    ApiCommand(CliEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Integer call() {
        RpcServer server = new RpcServer();
        StdioTransport transport = new StdioTransport(server);
        try (PianoApi api = PianoApi.create(server, environment.inputs(), environment.outputs(), environment.clock(),
                transport.events())) {
            transport.onEndOfInput(api::close);
            transport.run(environment.stdin(), environment.stdout());
        }
        return 0;
    }
}
