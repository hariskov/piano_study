package bg.petarh.piano.generator.chords;

import bg.petarh.piano.model.chord.Chord;
import bg.petarh.piano.model.chord.ChordType;
import bg.petarh.piano.model.chord.Inversion;
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
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RandomChordGeneratorTest {

    private static final Pitch MIDDLE_C = Pitch.parse("C4");
    private static final KeySignature D_MAJOR = KeySignature.major("D");

    @Test
    void sameSeedGivesTheSameChords() {
        ChordSettings settings = rightHand(EnumSet.allOf(ChordType.class), EnumSet.allOf(Inversion.class));

        assertThat(measures(RandomChordGenerator.withSeed(settings, 42), 8))
                .isEqualTo(measures(RandomChordGenerator.withSeed(settings, 42), 8));
    }

    @Test
    void randomSeedIsReportedSoTheExerciseCanBeReplayed() {
        ChordSettings settings = rightHand(EnumSet.allOf(ChordType.class), EnumSet.allOf(Inversion.class));
        RandomChordGenerator generator = RandomChordGenerator.withRandomSeed(settings);

        assertThat(measures(RandomChordGenerator.withSeed(settings, generator.seed()), 4))
                .isEqualTo(measures(generator, 4));
    }

    @Test
    void placesOneChordPerNoteValue() {
        RandomChordGenerator generator = RandomChordGenerator.withSeed(
                rightHand(Set.of(ChordType.MAJOR), Set.of(Inversion.ROOT_POSITION)), 3);

        List<ChordEvent> first = generator.nextMeasure();
        List<ChordEvent> second = generator.nextMeasure();

        assertThat(first).extracting(ChordEvent::start).containsExactly(Fraction.ZERO, Fraction.of(1, 2));
        assertThat(second).extracting(ChordEvent::start).containsExactly(Fraction.whole(1), Fraction.of(3, 2));
        assertThat(first).extracting(ChordEvent::duration).containsOnly(Fraction.of(1, 2));
    }

    @Test
    void usesOnlyTheChosenTypesAndInversions() {
        Set<ChordType> types = Set.of(ChordType.MINOR, ChordType.DOMINANT_SEVENTH);
        Set<Inversion> inversions = Set.of(Inversion.FIRST, Inversion.THIRD);

        List<Chord> chords = chords(rightHand(types, inversions), 9, 32);

        assertThat(chords).extracting(Chord::type).containsOnly(ChordType.MINOR, ChordType.DOMINANT_SEVENTH);
        assertThat(chords).extracting(Chord::inversion).containsOnly(Inversion.FIRST, Inversion.THIRD);
        assertThat(chords).filteredOn(chord -> chord.inversion() == Inversion.THIRD)
                .extracting(Chord::type).containsOnly(ChordType.DOMINANT_SEVENTH);
    }

    @Test
    void buildsChordsOnNotesOfTheKey() {
        assertThat(chords(rightHand(EnumSet.allOf(ChordType.class), EnumSet.allOf(Inversion.class)), 4, 32))
                .extracting(Chord::root).allMatch(D_MAJOR::contains);
    }

    @Test
    void neverRepeatsTheSameChordStraightAway() {
        List<Chord> chords = chords(rightHand(Set.of(ChordType.MAJOR), Set.of(Inversion.ROOT_POSITION)), 6, 32);

        assertThat(IntStream.range(1, chords.size())).allMatch(i -> !chords.get(i).equals(chords.get(i - 1)));
    }

    @Test
    void eachChordIsOneTargetForItsHand() {
        List<ChordEvent> measure = RandomChordGenerator.withSeed(
                rightHand(Set.of(ChordType.MAJOR_SEVENTH), Set.of(Inversion.SECOND)), 8).nextMeasure();

        List<Target> targets = Targets.from(measure.stream().flatMap(event -> event.notes().stream()).toList());

        assertThat(targets).hasSize(measure.size());
        assertThat(targets.getFirst().pitches()).containsExactlyElementsOf(measure.getFirst().chord().pitches());
        assertThat(measure.getFirst().notes()).extracting(NoteEvent::hand).containsOnly(Hand.RIGHT);
    }

    @Test
    void bothHandsTakeTurnsEachMeasureSplitAtMiddleC() {
        ChordSettings settings = new ChordSettings(Set.of(Hand.RIGHT, Hand.LEFT), range("C2", "C6"),
                KeyboardRange.fullPiano(), D_MAJOR, Set.of(ChordType.MAJOR, ChordType.MINOR),
                EnumSet.allOf(Inversion.class), NoteValue.HALF, TimeSignature.COMMON_TIME);
        RandomChordGenerator generator = RandomChordGenerator.withSeed(settings, 5);

        for (int measure = 0; measure < 8; measure++) {
            Hand expected = measure % 2 == 0 ? Hand.RIGHT : Hand.LEFT;
            List<ChordEvent> events = generator.nextMeasure();
            assertThat(events).extracting(ChordEvent::hand).containsOnly(expected);
            assertThat(events).flatExtracting(event -> event.chord().pitches()).allMatch(pitch -> expected == Hand.RIGHT
                    ? pitch.compareTo(MIDDLE_C) >= 0
                    : pitch.compareTo(MIDDLE_C) < 0);
        }
    }

    @Test
    void skipsChordsThatWouldGoAboveTheHighestMidiNote() {
        KeyboardRange top = KeyboardRange.of(Pitch.parse("C8"), Pitch.of(127));
        ChordSettings settings = new ChordSettings(Set.of(Hand.RIGHT), top, top, KeySignature.major("C"),
                EnumSet.allOf(ChordType.class), EnumSet.allOf(Inversion.class), NoteValue.QUARTER,
                TimeSignature.COMMON_TIME);

        assertThat(chords(settings, 1, 8)).flatExtracting(Chord::pitches).allMatch(top::contains);
    }

    @ParameterizedTest
    @MethodSource("keyboardsAndRanges")
    void neverPlaysOutsideTheRangeOrKeyboard(KeyboardRange keyboard, KeyboardRange practiceRange, long seed) {
        ChordSettings settings = new ChordSettings(Set.of(Hand.RIGHT), practiceRange, keyboard, D_MAJOR,
                EnumSet.allOf(ChordType.class), EnumSet.allOf(Inversion.class), NoteValue.QUARTER,
                TimeSignature.COMMON_TIME);

        assertThat(chords(settings, seed, 16)).flatExtracting(Chord::pitches)
                .allMatch(settings.playableRange()::contains);
    }

    static Stream<Arguments> keyboardsAndRanges() {
        SplittableRandom random = new SplittableRandom(2026);
        List<Arguments> cases = new ArrayList<>();
        for (KeyboardPreset preset : KeyboardPreset.values()) {
            for (int i = 0; i < 20; i++) {
                int low = random.nextInt(21, 90);
                KeyboardRange practice = KeyboardRange.of(Pitch.of(low), Pitch.of(random.nextInt(low + 18, 109)));
                if (practice.intersect(preset.range()).filter(r -> r.keyCount() >= 18).isPresent()) {
                    cases.add(Arguments.of(preset.range(), practice, random.nextLong()));
                }
            }
        }
        return cases.stream();
    }

    private static ChordSettings rightHand(Set<ChordType> types, Set<Inversion> inversions) {
        return new ChordSettings(Set.of(Hand.RIGHT), range("C4", "C6"), KeyboardRange.fullPiano(), D_MAJOR,
                types, inversions, NoteValue.HALF, TimeSignature.COMMON_TIME);
    }

    private static List<Chord> chords(ChordSettings settings, long seed, int measureCount) {
        return measures(RandomChordGenerator.withSeed(settings, seed), measureCount).stream()
                .flatMap(List::stream).map(ChordEvent::chord).toList();
    }

    private static List<List<ChordEvent>> measures(RandomChordGenerator generator, int count) {
        return IntStream.range(0, count).mapToObj(i -> generator.nextMeasure()).toList();
    }

    private static KeyboardRange range(String lowest, String highest) {
        return KeyboardRange.of(Pitch.parse(lowest), Pitch.parse(highest));
    }
}
