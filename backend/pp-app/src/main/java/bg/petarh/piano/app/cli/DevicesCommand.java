package bg.petarh.piano.app.cli;

import bg.petarh.piano.app.device.DeviceListing;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.output.MidiOutputPort;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

@Command(
        name = "devices",
        mixinStandardHelpOptions = true,
        description = "List MIDI input and output devices and their ids.")
final class DevicesCommand implements Callable<Integer> {

    private final MidiInputPort inputs;
    private final MidiOutputPort outputs;

    @Spec
    private CommandSpec spec;

    DevicesCommand(MidiInputPort inputs, MidiOutputPort outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        new DeviceListing(inputs, outputs).lines().forEach(out::println);
        return 0;
    }
}
