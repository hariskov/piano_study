package bg.petarh.piano.generator.notes;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardPreset;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.note.Targets;
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
import java.util.stream.Collectors;
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

    @Test
    void levelThreeAddsSomeTwoNoteIntervals() {
        KeySignature key = KeySignature.major("C");
        List<Pitch> scale = scaleIn(range("C4", "C6"), key);
        List<List<NoteEvent>> together = notesTogether(notes(rightHand(Level.THREE, key), 3, 32));

        assertThat(together).extracting(List::size).contains(1, 2).doesNotContain(3);
        assertThat(together).filteredOn(group -> group.size() == 2)
                .allMatch(group -> isBetween(scaleSteps(group, scale).getFirst(), 2, 5));
    }

    @Test
    void levelFourAddsThreeNoteChordsBuiltInThirds() {
        KeySignature key = KeySignature.major("C");
        List<Pitch> scale = scaleIn(range("C4", "C6"), key);
        List<List<NoteEvent>> together = notesTogether(notes(rightHand(Level.FOUR, key), 3, 32));

        assertThat(together).extracting(List::size).contains(1, 2, 3).allMatch(size -> size <= 3);
        assertThat(together).filteredOn(group -> group.size() == 3)
                .allMatch(group -> scaleSteps(group, scale).equals(List.of(2, 2)));
    }

    @Test
    void notesPlayedTogetherShareTheirDuration() {
        List<List<NoteEvent>> together = notesTogether(notes(rightHand(Level.FOUR, KeySignature.major("C")), 4, 16));

        assertThat(together).allMatch(group -> group.stream().map(NoteEvent::duration).distinct().count() == 1);
    }

    @Test
    void levelFourPlaysSharpsAndFlatsWhenAllowed() {
        KeySignature cMajor = KeySignature.major("C");

        assertThat(notes(withOptions(Level.FOUR, true, false), 2, 32))
                .anyMatch(note -> !cMajor.contains(note.pitch()));
    }

    @ParameterizedTest
    @EnumSource(value = Level.class, names = {"ONE", "TWO", "THREE"})
    void levelsBelowFourIgnoreAllowedAccidentals(Level level) {
        KeySignature cMajor = KeySignature.major("C");

        assertThat(notes(withOptions(level, true, false), 2, 32)).allMatch(note -> cMajor.contains(note.pitch()));
    }

    @ParameterizedTest
    @EnumSource(value = Level.class, names = {"THREE", "FOUR", "FIVE"})
    void mixedNoteValuesStillFillEachMeasure(Level level) {
        RandomNotesGenerator generator = RandomNotesGenerator.withSeed(withOptions(level, false, true), 12);

        List<Fraction> durations = new ArrayList<>();
        for (int measure = 0; measure < 16; measure++) {
            List<List<NoteEvent>> together = notesTogether(generator.nextMeasure());
            Fraction filled = together.stream().map(group -> group.getFirst().duration())
                    .reduce(Fraction.ZERO, Fraction::plus);
            assertThat(filled).isEqualTo(Fraction.whole(1));
            assertThat(together.getFirst().getFirst().start()).isEqualTo(Fraction.whole(measure));
            together.forEach(group -> durations.add(group.getFirst().duration()));
        }
        assertThat(durations).allMatch(duration -> duration.compareTo(Fraction.of(1, 4)) >= 0);
        assertThat(durations.stream().distinct().count()).isGreaterThan(1);
    }

    @Test
    void levelsBelowThreeIgnoreMixedNoteValues() {
        assertThat(notes(withOptions(Level.TWO, false, true), 2, 16)).extracting(NoteEvent::duration)
                .containsOnly(Fraction.of(1, 4));
    }

    @Test
    void levelFivePlaysBothHandsInEveryMeasure() {
        RandomNotesGenerator generator = RandomNotesGenerator.withSeed(bothHands(Level.FIVE, false), 21);

        for (int measure = 0; measure < 8; measure++) {
            List<NoteEvent> notes = generator.nextMeasure();
            for (Hand hand : Hand.values()) {
                List<NoteEvent> handNotes = notes.stream().filter(note -> note.hand() == hand).toList();
                assertThat(filledBy(handNotes)).isEqualTo(Fraction.whole(1));
                assertThat(handNotes).allMatch(note -> hand == Hand.RIGHT
                        ? note.pitch().compareTo(MIDDLE_C) >= 0
                        : note.pitch().compareTo(MIDDLE_C) < 0);
            }
        }
    }

    @Test
    void levelFiveHandsHaveTheirOwnRhythm() {
        RandomNotesGenerator generator = RandomNotesGenerator.withSeed(bothHands(Level.FIVE, true), 4);

        boolean anyMeasureDiffers = IntStream.range(0, 16).mapToObj(i -> generator.nextMeasure())
                .anyMatch(notes -> !startsOf(notes, Hand.RIGHT).equals(startsOf(notes, Hand.LEFT)));
        assertThat(anyMeasureDiffers).isTrue();
    }

    @Test
    void levelFiveListsNotesInTimeOrder() {
        List<NoteEvent> notes = RandomNotesGenerator.withSeed(bothHands(Level.FIVE, true), 9).nextMeasure();

        assertThat(notes).extracting(NoteEvent::start).isSorted();
    }

    @Test
    void levelFiveWithOneHandPlaysThatHandOnly() {
        assertThat(notes(withOptions(Level.FIVE, false, false), 3, 8)).extracting(NoteEvent::hand)
                .containsOnly(Hand.RIGHT);
    }

    @ParameterizedTest
    @MethodSource("keyboardsAndRanges")
    void neverPlaysOutsideTheKeyboard(KeyboardRange keyboard, KeyboardRange practiceRange, Level level, long seed) {
        RandomNotesSettings settings = new RandomNotesSettings(Set.of(Hand.RIGHT), practiceRange, keyboard,
                KeySignature.major("D"), NoteValue.QUARTER, TimeSignature.COMMON_TIME, level, true, true);

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

    private static RandomNotesSettings withOptions(Level level, boolean accidentals, boolean mixedNoteValues) {
        return new RandomNotesSettings(Set.of(Hand.RIGHT), range("C4", "C6"), KeyboardRange.fullPiano(),
                KeySignature.major("C"), NoteValue.QUARTER, TimeSignature.COMMON_TIME, level, accidentals,
                mixedNoteValues);
    }

    private static RandomNotesSettings bothHands(Level level, boolean mixedNoteValues) {
        return new RandomNotesSettings(Set.of(Hand.RIGHT, Hand.LEFT), range("C2", "C6"), KeyboardRange.fullPiano(),
                KeySignature.major("C"), NoteValue.QUARTER, TimeSignature.COMMON_TIME, level, false,
                mixedNoteValues);
    }

    private static Fraction filledBy(List<NoteEvent> notes) {
        return notesTogether(notes).stream().map(group -> group.getFirst().duration())
                .reduce(Fraction.ZERO, Fraction::plus);
    }

    private static Set<Fraction> startsOf(List<NoteEvent> notes, Hand hand) {
        return notes.stream().filter(note -> note.hand() == hand).map(NoteEvent::start)
                .collect(Collectors.toSet());
    }

    private static List<List<NoteEvent>> notesTogether(List<NoteEvent> notes) {
        return Targets.from(notes).stream().map(Target::notes).toList();
    }

    private static boolean isBetween(int value, int lowest, int highest) {
        return value >= lowest && value <= highest;
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
