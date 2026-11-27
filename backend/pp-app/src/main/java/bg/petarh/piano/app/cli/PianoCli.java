package bg.petarh.piano.app.cli;

import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.output.MidiOutputPort;
import picocli.CommandLine;

import java.io.PrintWriter;

public final class PianoCli {

    private PianoCli() {
    }

    public static CommandLine create(MidiInputPort inputs, MidiOutputPort outputs, PrintWriter out, PrintWriter err) {
        CommandLine commandLine = new CommandLine(new PianoCommand());
        commandLine.addSubcommand(new DevicesCommand(inputs, outputs));
        commandLine.setOut(out);
        commandLine.setErr(err);
        return commandLine;
    }
}
