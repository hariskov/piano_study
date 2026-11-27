package bg.petarh.piano.app.cli;

import bg.petarh.piano.app.monitor.InputEventFormatter;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.input.InputConnection;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

@Command(
        name = "monitor",
        mixinStandardHelpOptions = true,
        description = "Print every key press, release and pedal change from a MIDI input.")
final class MonitorCommand implements Callable<Integer> {

    private final CliEnvironment environment;

    @Spec
    private CommandSpec spec;

    @Parameters(paramLabel = "DEVICE", description = "Input device id, as shown by 'pp devices'.")
    private String deviceId;

    MonitorCommand(CliEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Integer call() throws Exception {
        PrintWriter out = spec.commandLine().getOut();
        InputEventFormatter formatter = new InputEventFormatter(environment.clock().nanos());
        InputConnection connection = environment.inputs()
                .open(DeviceId.of(deviceId), event -> out.println(formatter.format(event)));
        out.println("Monitoring " + deviceId + ". Press keys on the piano, Ctrl+C to stop.");
        environment.lifetime().keepOpenUntilStopped(connection);
        return 0;
    }
}
