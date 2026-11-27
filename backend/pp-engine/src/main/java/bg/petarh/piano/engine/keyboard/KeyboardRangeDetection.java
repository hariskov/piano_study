package bg.petarh.piano.engine.keyboard;

import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.pitch.Pitch;

public final class KeyboardRangeDetection {

    private volatile Progress progress = new Progress.AwaitingLowest();

    public KeyboardRangeDetection() {
    }

    public Progress progress() {
        return progress;
    }

    public synchronized Progress onEvent(InputEvent event) {
        if (event instanceof InputEvent.NoteOn press) {
            progress = advance(progress, press.pitch());
        }
        return progress;
    }

    private static Progress advance(Progress current, Pitch pressed) {
        return switch (current) {
            case Progress.AwaitingLowest ignored -> new Progress.AwaitingHighest(pressed);
            case Progress.AwaitingHighest awaiting when awaiting.first().equals(pressed) -> awaiting;
            case Progress.AwaitingHighest awaiting ->
                    new Progress.Detected(KeyboardRange.spanning(awaiting.first(), pressed));
            case Progress.Detected detected -> detected;
        };
    }

    public sealed interface Progress {

        record AwaitingLowest() implements Progress {
        }

        record AwaitingHighest(Pitch first) implements Progress {
        }

        record Detected(KeyboardRange range) implements Progress {
        }
    }
}
