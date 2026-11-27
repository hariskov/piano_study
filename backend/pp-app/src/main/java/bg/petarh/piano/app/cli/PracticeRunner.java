package bg.petarh.piano.app.cli;

import bg.petarh.piano.app.practice.PracticePrinter;
import bg.petarh.piano.engine.metronome.ClickOutput;
import bg.petarh.piano.engine.metronome.MidiClicks;
import bg.petarh.piano.engine.run.Practice;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.time.Tempo;

import java.io.PrintWriter;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

final class PracticeRunner {

    private final CliEnvironment environment;
    private final PrintWriter out;

    PracticeRunner(CliEnvironment environment, PrintWriter out) {
        this.environment = environment;
        this.out = out;
    }

    static SessionTiming timing(PracticeOptions options) {
        return new SessionTiming(Tempo.of(options.bpm), options.meter, options.countInBars);
    }

    void run(PracticeOptions options, SessionTiming timing, Supplier<List<NoteEvent>> measures,
             Function<Target, String> names) throws Exception {
        TargetSource source = TargetSource.fromMeasures(measures);
        TargetSource targets = options.targetCount > 0 ? source.limitedTo(options.targetCount) : source;
        PracticePrinter printer = new PracticePrinter(out, names);
        Optional<OutputConnection> metronomeOutput = options.metronomeOutputId
                .map(id -> environment.outputs().open(DeviceId.of(id)));
        AutoCloseable metronome = () -> metronomeOutput.ifPresent(OutputConnection::close);
        Practice practice = Practice.prepare(targets, timing, options.mode, environment.clock()::nanos, printer,
                metronomeOutput.<ClickOutput>map(MidiClicks::new));
        InputConnection input = openInput(options, practice, metronome);
        out.println(options.mode.name().charAt(0) + options.mode.name().substring(1).toLowerCase(Locale.ROOT)
                + " mode at " + timing.tempo() + ". Ctrl+C to stop.");
        CompletionStage<Void> done = practice.start();
        environment.lifetime().keepOpenUntil(() -> {
            try (metronome; practice; input) {
                // Input closes first so no key press reaches a stopped session, and the metronome output last.
            }
        }, done);
        out.println(printer.tally().summary(Duration.ofNanos(practice.elapsedNanos())));
    }

    private InputConnection openInput(PracticeOptions options, Practice practice, AutoCloseable metronome)
            throws Exception {
        try {
            return environment.inputs().open(DeviceId.of(options.inputId), practice.input());
        } catch (RuntimeException e) {
            metronome.close();
            throw e;
        }
    }
}
