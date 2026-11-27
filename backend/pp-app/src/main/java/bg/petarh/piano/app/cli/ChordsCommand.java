package bg.petarh.piano.app.cli;

import bg.petarh.piano.app.practice.TargetNames;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.generator.chords.ChordEvent;
import bg.petarh.piano.generator.chords.ChordSettings;
import bg.petarh.piano.generator.chords.RandomChordGenerator;
import bg.petarh.piano.model.chord.Chord;
import bg.petarh.piano.model.chord.ChordType;
import bg.petarh.piano.model.chord.Inversion;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Command(
        name = "chords",
        mixinStandardHelpOptions = true,
        description = "Practise playing random chords on the piano.")
final class ChordsCommand implements Callable<Integer> {

    enum Show { BOTH, NAME, NOTES }

    private final CliEnvironment environment;
    private final Map<Fraction, Chord> chordsByStart = new ConcurrentHashMap<>();

    @Spec
    private CommandSpec spec;

    @Mixin
    private PracticeOptions options;

    @Option(names = "--types", split = ",", defaultValue = "maj,min", converter = PracticeConverters.ChordTypeConverter.class,
            description = "Chord types: maj, min, dim, aug, maj7, min7, dom7 (default: ${DEFAULT-VALUE}).")
    private List<ChordType> types;

    @Option(names = "--inversions", split = ",", defaultValue = "0",
            description = "Inversions: 0 (root position), 1, 2, 3 (default: ${DEFAULT-VALUE}).")
    private List<Integer> inversions;

    @Option(names = "--show", defaultValue = "both",
            description = "What to show: both, name or notes (default: ${DEFAULT-VALUE}).")
    private Show show;

    ChordsCommand(CliEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Integer call() throws Exception {
        PrintWriter out = spec.commandLine().getOut();
        RandomChordGenerator generator;
        SessionTiming timing;
        try {
            ChordSettings settings = new ChordSettings(options.handSet(), options.practiceRange(), options.keyboard,
                    options.key, EnumSet.copyOf(types), inversionSet(), options.noteValue, options.meter);
            generator = options.seed
                    .map(seed -> RandomChordGenerator.withSeed(settings, seed))
                    .orElseGet(() -> RandomChordGenerator.withRandomSeed(settings));
            timing = PracticeRunner.timing(options);
        } catch (IllegalArgumentException e) {
            throw new ParameterException(spec.commandLine(), e.getMessage(), e);
        }
        out.println("Chords " + types.stream().map(ChordType::code).collect(Collectors.joining(", "))
                + " in " + options.key + ", inversions " + inversions.stream().map(String::valueOf)
                .collect(Collectors.joining(", ")) + ". Seed " + generator.seed()
                + " (use --seed " + generator.seed() + " to replay).");
        new PracticeRunner(environment, out).run(options, timing, () -> nextMeasure(generator), this::describe);
        return 0;
    }

    private EnumSet<Inversion> inversionSet() {
        return inversions.stream().map(Inversion::of).collect(Collectors.toCollection(() -> EnumSet.noneOf(Inversion.class)));
    }

    private List<NoteEvent> nextMeasure(RandomChordGenerator generator) {
        List<ChordEvent> measure = generator.nextMeasure();
        measure.forEach(event -> chordsByStart.put(event.start(), event.chord()));
        return measure.stream().flatMap(event -> event.notes().stream()).toList();
    }

    private String describe(Target target) {
        Optional<Chord> chord = Optional.ofNullable(chordsByStart.remove(target.start()));
        if (chord.isEmpty()) {
            return TargetNames.describe(target);
        }
        String name = chord.get().name(options.key);
        String notes = chord.get().pitches().stream().map(Pitch::name).collect(Collectors.joining(" "));
        return switch (show) {
            case BOTH -> name + "  (" + notes + ")";
            case NAME -> name;
            case NOTES -> notes;
        };
    }
}
