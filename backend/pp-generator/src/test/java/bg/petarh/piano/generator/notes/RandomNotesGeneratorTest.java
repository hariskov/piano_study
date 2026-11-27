package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardPreset;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.NoteValue;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RandomNotesGeneratorTest {

    private static final Pitch MIDDLE_C = Pitch.parse("C4");

    @Test
    void sameSeedGivesTheSameNotes() {
        RandomNotesSettings settings = rightHand(Level.TWO, KeySignature.major("C"));

        assertThat(measures(RandomNotesGenerator.withSeed(settings, 42), 8))
                .isEqualTo(measures(RandomNotesGenerator.withSeed(settings, 42), 8));
    }

    @Test
    void differentSeedsGiveDifferentNotes() {
        RandomNotesSettings settings = rightHand(Level.TWO, KeySignature.major("C"));

        assertThat(measures(RandomNotesGenerator.withSeed(settings, 1), 8))
                .isNotEqualTo(measures(RandomNotesGenerator.withSeed(settings, 2), 8));
    }

    @Test
    void randomSeedIsReportedSoTheExerciseCanBeReplayed() {
        RandomNotesSettings settings = rightHand(Level.TWO, KeySignature.major("C"));
        RandomNotesGenerator generator = RandomNotesGenerator.withRandomSeed(settings);

        assertThat(measures(RandomNotesGenerator.withSeed(settings, generator.seed()), 4))
                .isEqualTo(measures(generator, 4));
    }

    @Test
    void fillsEachMeasureWithNotesOfTheChosenValue() {
        RandomNotesSettings settings = new RandomNotesSettings(Set.of(Hand.RIGHT), range("C4", "C6"),
                KeyboardRange.fullPiano(), KeySignature.major("C"), NoteValue.EIGHTH,
                TimeSignature.parse("6/8"), Level.ONE);
        RandomNotesGenerator generator = RandomNotesGenerator.withSeed(settings, 7);

        List<NoteEvent> first = generator.nextMeasure();
        List<NoteEvent> second = generator.nextMeasure();

        assertThat(first).extracting(NoteEvent::start).containsExactly(
                Fraction.ZERO, eighths(1), eighths(2), eighths(3), eighths(4), eighths(5));
        assertThat(second.getFirst().start()).isEqualTo(eighths(6));
        assertThat(first).extracting(NoteEvent::duration).containsOnly(Fraction.of(1, 8));
    }

    @ParameterizedTest
    @EnumSource(Level.class)
    void playsOnlyNotesOfTheKey(Level level) {
        KeySignature eFlatMajor = KeySignature.major("Eb");

        assertThat(notes(rightHand(level, eFlatMajor), 3, 32)).allMatch(note -> eFlatMajor.contains(note.pitch()));
    }

    @Test
    void levelOneMovesBySecondsAndThirds() {
        KeySignature key = KeySignature.major("G");
        List<Pitch> scale = scaleIn(range("C4", "C6"), key);
        List<NoteEvent> notes = notes(rightHand(Level.ONE, key), 5, 32);

        assertThat(scaleSteps(notes, scale)).allMatch(step -> step == 1 || step == 2);
    }

    @Test
    void levelOneStaysWithinAnOctaveOfNotes() {
        List<NoteEvent> notes = notes(rightHand(Level.ONE, KeySignature.major("C")), 9, 32);

        assertThat(notes).extracting(NoteEvent::pitch).allMatch(pitch -> pitch.compareTo(Pitch.parse("C5")) <= 0);
    }

    @Test
    void levelTwoJumpsUpToAnOctave() {
        List<NoteEvent> notes = notes(rightHand(Level.TWO, KeySignature.major("C")), 11, 64);

        List<Integer> jumps = IntStream.range(1, notes.size())
                .map(i -> Math.abs(notes.get(i).pitch().midiNumber() - notes.get(i - 1).pitch().midiNumber()))
                .boxed().toList();
        assertThat(jumps).allMatch(jump -> jump >= 1 && jump <= 12);
        assertThat(jumps).anyMatch(jump -> jump > 4);
    }

    @Test
    void singleHandUsesTheWholeRange() {
        RandomNotesSettings settings = new RandomNotesSettings(Set.of(Hand.LEFT), range("C3", "C5"),
                KeyboardRange.fullPiano(), KeySignature.major("C"), NoteValue.QUARTER,
                TimeSignature.COMMON_TIME, Level.TWO);

        List<NoteEvent> notes = notes(settings, 3, 64);

        assertThat(notes).extracting(NoteEvent::hand).containsOnly(Hand.LEFT);
        assertThat(notes).anyMatch(note -> note.pitch().compareTo(MIDDLE_C) > 0);
    }

    @Test
    void bothHandsTakeTurnsEachMeasureSplitAtMiddleC() {
        RandomNotesSettings settings = new RandomNotesSettings(Set.of(Hand.RIGHT, Hand.LEFT), range("C2", "C6"),
                KeyboardRange.fullPiano(), KeySignature.major("C"), NoteValue.QUARTER,
                TimeSignature.COMMON_TIME, Level.TWO);
        RandomNotesGenerator generator = RandomNotesGenerator.withSeed(settings, 5);

        for (int measure = 0; measure < 8; measure++) {
            Hand expected = measure % 2 == 0 ? Hand.RIGHT : Hand.LEFT;
            List<NoteEvent> notes = generator.nextMeasure();
            assertThat(notes).extracting(NoteEvent::hand).containsOnly(expected);
            assertThat(notes).allMatch(note -> expected == Hand.RIGHT
                    ? note.pitch().compareTo(MIDDLE_C) >= 0
                    : note.pitch().compareTo(MIDDLE_C) < 0);
        }
    }

    @ParameterizedTest
    @MethodSource("keyboardsAndRanges")
    void neverPlaysOutsideTheKeyboard(KeyboardRange keyboard, KeyboardRange practiceRange, Level level, long seed) {
        RandomNotesSettings settings = new RandomNotesSettings(Set.of(Hand.RIGHT), practiceRange, keyboard,
                KeySignature.major("D"), NoteValue.QUARTER, TimeSignature.COMMON_TIME, level);

        assertThat(notes(settings, seed, 16)).extracting(NoteEvent::pitch).allMatch(keyboard::contains);
    }

    static Stream<Arguments> keyboardsAndRanges() {
        SplittableRandom random = new SplittableRandom(2026);
        List<Arguments> cases = new ArrayList<>();
        for (KeyboardPreset preset : KeyboardPreset.values()) {
            for (Level level : Level.values()) {
                for (int i = 0; i < 10; i++) {
                    int low = random.nextInt(21, 97);
                    KeyboardRange practice = KeyboardRange.of(Pitch.of(low), Pitch.of(random.nextInt(low + 12, 109)));
                    if (practice.intersect(preset.range()).filter(r -> r.keyCount() >= 4).isPresent()) {
                        cases.add(Arguments.of(preset.range(), practice, level, random.nextLong()));
                    }
                }
            }
        }
        return cases.stream();
    }

    private static RandomNotesSettings rightHand(Level level, KeySignature key) {
        return new RandomNotesSettings(Set.of(Hand.RIGHT), range("C4", "C6"), KeyboardRange.fullPiano(), key,
                NoteValue.QUARTER, TimeSignature.COMMON_TIME, level);
    }

    private static List<NoteEvent> notes(RandomNotesSettings settings, long seed, int measureCount) {
        return measures(RandomNotesGenerator.withSeed(settings, seed), measureCount).stream()
                .flatMap(List::stream).toList();
    }

    private static List<List<NoteEvent>> measures(RandomNotesGenerator generator, int count) {
        return IntStream.range(0, count).mapToObj(i -> generator.nextMeasure()).toList();
    }

    private static List<Pitch> scaleIn(KeyboardRange range, KeySignature key) {
        return range.pitches().stream().filter(key::contains).toList();
    }

    private static List<Integer> scaleSteps(List<NoteEvent> notes, List<Pitch> scale) {
        return IntStream.range(1, notes.size())
                .map(i -> Math.abs(scale.indexOf(notes.get(i).pitch()) - scale.indexOf(notes.get(i - 1).pitch())))
                .boxed().toList();
    }

    private static Fraction eighths(int count) {
        return Fraction.of(count, 8);
    }

    private static KeyboardRange range(String lowest, String highest) {
        return KeyboardRange.of(Pitch.parse(lowest), Pitch.parse(highest));
    }
}
