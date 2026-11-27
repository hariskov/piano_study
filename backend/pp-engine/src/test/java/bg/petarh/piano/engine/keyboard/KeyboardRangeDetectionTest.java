package bg.petarh.piano.engine.keyboard;

import bg.petarh.piano.engine.keyboard.KeyboardRangeDetection.Progress;
import bg.petarh.piano.midi.input.InputEvent;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeyboardRangeDetectionTest {

    private final KeyboardRangeDetection detection = new KeyboardRangeDetection();

    @Test
    void startsByWaitingForTheLowestKey() {
        assertThat(detection.progress()).isEqualTo(new Progress.AwaitingLowest());
    }

    @Test
    void firstPressedKeyBecomesTheLowest() {
        Progress progress = detection.onEvent(press("A0"));

        assertThat(progress).isEqualTo(new Progress.AwaitingHighest(Pitch.parse("A0")));
    }

    @Test
    void secondPressedKeyCompletesTheRange() {
        detection.onEvent(press("A0"));

        Progress progress = detection.onEvent(press("C8"));

        assertThat(progress).isEqualTo(
                new Progress.Detected(KeyboardRange.of(Pitch.parse("A0"), Pitch.parse("C8"))));
    }

    @Test
    void keysPressedInReverseOrderStillGiveTheRange() {
        detection.onEvent(press("C8"));

        Progress progress = detection.onEvent(press("A0"));

        assertThat(progress).isEqualTo(
                new Progress.Detected(KeyboardRange.of(Pitch.parse("A0"), Pitch.parse("C8"))));
    }

    @Test
    void ignoresReleasesAndPedal() {
        detection.onEvent(new InputEvent.NoteOff(Pitch.parse("A0"), 0L));
        detection.onEvent(new InputEvent.SustainPedal(true, 0L));

        assertThat(detection.progress()).isEqualTo(new Progress.AwaitingLowest());
    }

    @Test
    void ignoresTheFirstKeyPressedAgain() {
        detection.onEvent(press("A0"));

        Progress progress = detection.onEvent(press("A0"));

        assertThat(progress).isEqualTo(new Progress.AwaitingHighest(Pitch.parse("A0")));
    }

    @Test
    void keepsTheResultOnceDetected() {
        detection.onEvent(press("A0"));
        detection.onEvent(press("C8"));

        Progress progress = detection.onEvent(press("C4"));

        assertThat(progress).isEqualTo(
                new Progress.Detected(KeyboardRange.of(Pitch.parse("A0"), Pitch.parse("C8"))));
    }

    private static InputEvent press(String note) {
        return new InputEvent.NoteOn(Pitch.parse(note), Velocity.of(64), 0L);
    }
}
