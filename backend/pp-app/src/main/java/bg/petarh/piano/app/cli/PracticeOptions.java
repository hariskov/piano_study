package bg.petarh.piano.app.cli;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.NoteValue;
import bg.petarh.piano.model.time.TimeSignature;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

final class PracticeOptions {

    enum Hands { RIGHT, LEFT, BOTH }

    enum Mode { WAIT, TEMPO }

    @Parameters(index = "0", paramLabel = "INPUT", description = "Input device id, as shown by 'pp devices'.")
    String inputId;

    @Option(names = "--hands", defaultValue = "right", description = "right, left or both (default: ${DEFAULT-VALUE}).")
    Hands hands;

    @Option(names = "--mode", defaultValue = "wait", description = "wait or tempo (default: ${DEFAULT-VALUE}).")
    Mode mode;

    @Option(names = "--tempo", defaultValue = "90", description = "Beats per minute, 20-240 (default: ${DEFAULT-VALUE}).")
    int bpm;

    @Option(names = "--key", defaultValue = "C", converter = PracticeConverters.KeyConverter.class,
            description = "Key, e.g. C, Bb, F#m (default: ${DEFAULT-VALUE}).")
    KeySignature key;

    @Option(names = "--range", converter = PracticeConverters.RangeConverter.class,
            description = "Notes to practise, e.g. C4-C6 (default: C4-C6 right, C2-C4 left, C2-C6 both).")
    Optional<KeyboardRange> range;

    @Option(names = "--keyboard", defaultValue = "88", converter = PracticeConverters.KeyboardConverter.class,
            description = "Keyboard size (88, 76, 73, 61, 49, 37, 25) or range, e.g. F1-F7 (default: ${DEFAULT-VALUE}).")
    KeyboardRange keyboard;

    @Option(names = "--note-value", defaultValue = "quarter",
            description = "whole, half, quarter or eighth (default: ${DEFAULT-VALUE}).")
    NoteValue noteValue;

    @Option(names = "--time", defaultValue = "4/4", converter = PracticeConverters.TimeSignatureConverter.class,
            description = "Time signature, e.g. 3/4 or 6/8 (default: ${DEFAULT-VALUE}).")
    TimeSignature meter;

    @Option(names = "--notes", defaultValue = "50", description = "Number of targets, 0 for endless (default: ${DEFAULT-VALUE}).")
    int targetCount;

    @Option(names = "--count-in", defaultValue = "1", description = "Bars of count-in: 0, 1 or 2 (default: ${DEFAULT-VALUE}).")
    int countInBars;

    @Option(names = "--seed", description = "Seed to replay an exercise (default: random).")
    Optional<Long> seed;

    PracticeOptions() {
    }

    Set<Hand> handSet() {
        return switch (hands) {
            case RIGHT -> EnumSet.of(Hand.RIGHT);
            case LEFT -> EnumSet.of(Hand.LEFT);
            case BOTH -> EnumSet.allOf(Hand.class);
        };
    }

    KeyboardRange practiceRange() {
        return range.orElseGet(() -> switch (hands) {
            case RIGHT -> KeyboardRange.of(Pitch.parse("C4"), Pitch.parse("C6"));
            case LEFT -> KeyboardRange.of(Pitch.parse("C2"), Pitch.parse("C4"));
            case BOTH -> KeyboardRange.of(Pitch.parse("C2"), Pitch.parse("C6"));
        });
    }
}
