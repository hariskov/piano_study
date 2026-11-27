package bg.petarh.piano.app;

import bg.petarh.piano.app.cli.PianoCli;
import bg.petarh.piano.midi.jdk.JdkMidi;

import java.io.PrintWriter;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        PrintWriter out = new PrintWriter(System.out, true);
        PrintWriter err = new PrintWriter(System.err, true);
        int exitCode = PianoCli.create(JdkMidi.inputPort(), JdkMidi.outputPort(), out, err).execute(args);
        System.exit(exitCode);
    }
}
