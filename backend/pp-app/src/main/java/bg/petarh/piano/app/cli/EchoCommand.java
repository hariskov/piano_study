package bg.petarh.piano.app.cli;

import bg.petarh.piano.midi.output.InputEcho;
import bg.petarh.piano.app.monitor.InputEventFormatter;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.midi.input.InputListener;
import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.midi.value.MidiChannel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

@Command(
        name = "echo",
        mixinStandardHelpOptions = true,
        description = "Play everything pressed on a MIDI input through a MIDI output, e.g. the built-in synthesizer.")
final class EchoCommand implements Callable<Integer> {

    private static final MidiChannel ECHO_CHANNEL = MidiChannel.of(0);

    private final CliEnvironment environment;

    @Spec
    private CommandSpec spec;

    @Parameters(index = "0", paramLabel = "INPUT", description = "Input device id, as shown by 'pp devices'.")
    private String inputId;

    @Parameters(index = "1", paramLabel = "OUTPUT", description = "Output device id, e.g. jdk:Gervill.")
    private String outputId;

    EchoCommand(CliEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Integer call() throws Exception {
        PrintWriter out = spec.commandLine().getOut();
        InputEventFormatter formatter = new InputEventFormatter(environment.clock().nanos());
        OutputConnection output = environment.outputs().open(DeviceId.of(outputId));
        InputEcho echo = new InputEcho(output, ECHO_CHANNEL);
        InputConnection input = openInput(output, event -> {
            echo.onEvent(event);
            out.println(formatter.format(event));
        });
        out.println("Echoing " + inputId + " to " + outputId + ". Play the piano, Ctrl+C to stop.");
        environment.lifetime().keepOpenUntilStopped(() -> {
            try (output; input) {
                // Input closes first so no event reaches an output that is already closed.
            }
        });
        return 0;
    }

    private InputConnection openInput(OutputConnection output, InputListener listener) {
        try {
            return environment.inputs().open(DeviceId.of(inputId), listener);
        } catch (RuntimeException e) {
            output.close();
            throw e;
        }
    }
}
