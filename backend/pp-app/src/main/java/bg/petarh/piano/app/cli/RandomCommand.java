package bg.petarh.piano.app.cli;

import bg.petarh.piano.app.practice.TargetNames;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.generator.notes.Level;
import bg.petarh.piano.generator.notes.RandomNotesGenerator;
import bg.petarh.piano.generator.notes.RandomNotesOptions;
import bg.petarh.piano.generator.notes.RandomNotesSettings;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.util.concurrent.Callable;

@Command(
        name = "random",
        mixinStandardHelpOptions = true,
        description = "Practise reading random notes on the piano.")
final class RandomCommand implements Callable<Integer> {

    private final CliEnvironment environment;

    @Spec
    private CommandSpec spec;

    @Mixin
    private PracticeOptions options;

    @Option(names = "--level", defaultValue = "1", description = "Complexity level 1-5 (default: ${DEFAULT-VALUE}).")
    private int level;

    @Option(names = "--accidentals", description = "Allow sharps and flats outside the key (level 4 and up).")
    private boolean accidentals;

    @Option(names = "--mixed", description = "Mix note values (level 3 and up).")
    private boolean mixedNoteValues;

    RandomCommand(CliEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Integer call() throws Exception {
        PrintWriter out = spec.commandLine().getOut();
        RandomNotesGenerator generator;
        SessionTiming timing;
        try {
            RandomNotesSettings settings = new RandomNotesSettings(options.handSet(), options.practiceRange(),
                    options.keyboard, options.key, options.noteValue, options.meter, Level.of(level),
                    RandomNotesOptions.defaults().withAccidentals(accidentals).withMixedNoteValues(mixedNoteValues));
            generator = options.seed
                    .map(seed -> RandomNotesGenerator.withSeed(settings, seed))
                    .orElseGet(() -> RandomNotesGenerator.withRandomSeed(settings));
            timing = PracticeRunner.timing(options);
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), e.getMessage(), e);
        }
        out.println("Random notes in " + options.key + ", level " + level + ". Seed " + generator.seed()
                + " (use --seed " + generator.seed() + " to replay).");
        new PracticeRunner(environment, out).run(options, timing, generator::nextMeasure, TargetNames::describe);
        return 0;
    }
}
