package bg.petarh.piano.app.cli;

import bg.petarh.piano.app.practice.PracticePrinter;
import bg.petarh.piano.engine.evaluation.Evaluator;
import bg.petarh.piano.engine.evaluation.TempoEvaluator;
import bg.petarh.piano.engine.evaluation.WaitEvaluator;
import bg.petarh.piano.engine.run.SessionDriver;
import bg.petarh.piano.engine.run.SessionThread;
import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.engine.time.Clock;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.time.Tempo;

import java.io.PrintWriter;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

final class PracticeRunner {

    private static final Duration TICK = Duration.ofMillis(5);

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
        Clock clock = environment.clock()::nanos;
        TargetSource source = TargetSource.fromMeasures(measures);
        Session session = new Session(options.targetCount > 0 ? source.limitedTo(options.targetCount) : source,
                timing, clock);
        PracticePrinter printer = new PracticePrinter(out, names);
        SessionThread thread = new SessionThread(
                new SessionDriver(session, evaluator(options, session), clock, printer), TICK);
        InputConnection input = environment.inputs().open(DeviceId.of(options.inputId), thread);
        out.println(options.mode.name().charAt(0) + options.mode.name().substring(1).toLowerCase()
                + " mode at " + timing.tempo() + ". Ctrl+C to stop.");
        CompletionStage<Void> done = thread.start();
        environment.lifetime().keepOpenUntil(() -> {
            try (thread; input) {
                // Input closes first so no key press reaches a stopped session.
            }
        }, done);
        out.println(printer.tally().summary(Duration.ofNanos(session.elapsedNanos())));
    }

    private static Evaluator evaluator(PracticeOptions options, Session session) {
        return switch (options.mode) {
            case WAIT -> new WaitEvaluator(session);
            case TEMPO -> new TempoEvaluator(session);
        };
    }
}
