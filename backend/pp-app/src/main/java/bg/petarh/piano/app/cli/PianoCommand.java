package bg.petarh.piano.app.cli;

import picocli.CommandLine.Command;

@Command(
        name = "pp",
        mixinStandardHelpOptions = true,
        version = "pp 0.1.0",
        description = "Piano practice tools.")
final class PianoCommand {
}
