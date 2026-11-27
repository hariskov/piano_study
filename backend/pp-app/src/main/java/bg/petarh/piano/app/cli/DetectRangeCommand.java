package bg.petarh.piano.app.cli;

import bg.petarh.piano.engine.keyboard.KeyboardRangeDetection;
import bg.petarh.piano.engine.keyboard.KeyboardRangeDetection.Progress;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.input.InputConnection;
import bg.petarh.piano.model.keyboard.KeyboardPreset;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Command(
        name = "detect-range",
        mixinStandardHelpOptions = true,
        description = "Find the keyboard's range by pressing its lowest and highest key.")
final class DetectRangeCommand implements Callable<Integer> {

    private final CliEnvironment environment;

    @Spec
    private CommandSpec spec;

    @Parameters(paramLabel = "DEVICE", description = "Input device id, as shown by 'pp devices'.")
    private String deviceId;

    DetectRangeCommand(CliEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public Integer call() throws Exception {
        PrintWriter out = spec.commandLine().getOut();
        KeyboardRangeDetection detection = new KeyboardRangeDetection();
        AtomicReference<Progress> shown = new AtomicReference<>(detection.progress());
        CompletableFuture<KeyboardRange> detected = new CompletableFuture<>();

        InputConnection connection = environment.inputs().open(DeviceId.of(deviceId), event -> {
            Progress progress = detection.onEvent(event);
            if (!progress.equals(shown.getAndSet(progress))) {
                switch (progress) {
                    case Progress.AwaitingLowest ignored -> {
                    }
                    case Progress.AwaitingHighest awaiting ->
                            out.println("Lowest key: " + awaiting.first() + ". Now press the highest key.");
                    case Progress.Detected done -> detected.complete(done.range());
                }
            }
        });
        out.println("Press the lowest key on your keyboard.");
        environment.lifetime().keepOpenUntil(connection, detected);

        KeyboardRange range = detected.join();
        out.println("Detected range: " + range + ", " + range.keyCount() + " keys (" + describe(range) + ").");
        return 0;
    }

    private static String describe(KeyboardRange range) {
        return KeyboardPreset.matching(range)
                .map(preset -> "matches the " + preset.keyCount() + "-key preset")
                .orElse("custom range");
    }
}
