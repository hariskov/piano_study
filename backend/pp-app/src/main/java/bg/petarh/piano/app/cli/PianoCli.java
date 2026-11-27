package bg.petarh.piano.app.cli;

import bg.petarh.piano.importmidi.MidiImportException;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.musicxml.MusicXmlException;
import picocli.CommandLine;

import java.io.PrintWriter;

public final class PianoCli {

    private static final int DEVICE_ERROR = 1;
    private static final int FILE_ERROR = 1;

    private PianoCli() {
    }

    public static CommandLine create(CliEnvironment environment, PrintWriter out, PrintWriter err) {
        CommandLine commandLine = new CommandLine(new PianoCommand());
        commandLine.addSubcommand(new DevicesCommand(environment.inputs(), environment.outputs()));
        commandLine.addSubcommand(new MonitorCommand(environment));
        commandLine.addSubcommand(new EchoCommand(environment));
        commandLine.addSubcommand(new DetectRangeCommand(environment));
        commandLine.addSubcommand(new RandomCommand(environment));
        commandLine.addSubcommand(new ChordsCommand(environment));
        commandLine.addSubcommand(new ApiCommand(environment));
        commandLine.addSubcommand(new ImportCommand());
        commandLine.addSubcommand(new SongsCommand());
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        commandLine.setOut(out);
        commandLine.setErr(err);
        commandLine.setExecutionExceptionHandler((exception, command, parseResult) -> {
            if (exception instanceof DeviceUnavailableException) {
                err.println("Error: " + exception.getMessage() + ". Run 'pp devices' to see the available devices.");
                return DEVICE_ERROR;
            }
            if (exception instanceof MidiImportException || exception instanceof MusicXmlException) {
                err.println("Error: " + exception.getMessage());
                return FILE_ERROR;
            }
            throw exception;
        });
        return commandLine;
    }
}
