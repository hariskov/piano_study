package bg.petarh.piano.app;

import bg.petarh.piano.app.cli.CliEnvironment;
import bg.petarh.piano.app.cli.PianoCli;
import bg.petarh.piano.app.lifetime.UntilShutdown;
import bg.petarh.piano.midi.input.TimestampSource;
import bg.petarh.piano.midi.jdk.JdkMidi;

import java.io.PrintWriter;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        PrintWriter out = new PrintWriter(System.out, true);
        PrintWriter err = new PrintWriter(System.err, true);
        TimestampSource clock = TimestampSource.system();
        CliEnvironment environment = new CliEnvironment(
                JdkMidi.inputPort(clock), JdkMidi.outputPort(), clock, new UntilShutdown(err));
        System.exit(PianoCli.create(environment, out, err).execute(args));
    }
}
