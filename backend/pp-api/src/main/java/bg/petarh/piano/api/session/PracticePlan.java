package bg.petarh.piano.api.session;

import bg.petarh.piano.engine.evaluation.FollowMode;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.generator.chords.ChordEvent;
import bg.petarh.piano.generator.chords.ChordSettings;
import bg.petarh.piano.generator.chords.RandomChordGenerator;
import bg.petarh.piano.generator.notes.Level;
import bg.petarh.piano.generator.notes.RandomNotesGenerator;
import bg.petarh.piano.generator.notes.RandomNotesOptions;
import bg.petarh.piano.generator.notes.RandomNotesSettings;
import bg.petarh.piano.model.chord.Chord;
import bg.petarh.piano.model.chord.ChordType;
import bg.petarh.piano.model.chord.Inversion;
import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.NoteValue;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

record PracticePlan(TargetSource targets, SessionTiming timing, FollowMode mode, long seed,
                    Function<Target, Optional<String>> names) {

    private static final int DEFAULT_TEMPO = 90;
    private static final int DEFAULT_NOTES = 50;
    private static final int DEFAULT_COUNT_IN_BARS = 1;

    static PracticePlan from(SessionStartParams params) {
        if (params.input() == null) {
            throw new IllegalArgumentException("input is required: the MIDI input device id");
        }
        FollowMode mode = FollowMode.valueOf(or(params.mode(), "wait").toUpperCase(Locale.ROOT));
        SessionTiming timing = new SessionTiming(Tempo.of(or(params.tempo(), DEFAULT_TEMPO)),
                TimeSignature.parse(or(params.time(), "4/4")), or(params.countIn(), DEFAULT_COUNT_IN_BARS));
        long seed = params.seed() != null ? params.seed() : ThreadLocalRandom.current().nextLong();
        Set<Hand> hands = hands(or(params.hands(), "right"));
        KeyboardRange range = params.range() != null ? KeyboardRange.parse(params.range()) : defaultRange(hands);
        KeyboardRange keyboard = KeyboardRange.parseKeyboard(or(params.keyboard(), "88"));
        KeySignature key = KeySignature.parse(or(params.key(), "C"));
        NoteValue noteValue = NoteValue.valueOf(or(params.noteValue(), "quarter").toUpperCase(Locale.ROOT));
        int notes = or(params.notes(), DEFAULT_NOTES);
        return switch (or(params.exercise(), "")) {
            case "random" -> {
                RandomNotesSettings settings = new RandomNotesSettings(hands, range, keyboard, key, noteValue,
                        timing.meter(), Level.of(or(params.level(), 1)), RandomNotesOptions.defaults()
                        .withAccidentals(or(params.accidentals(), false))
                        .withMixedNoteValues(or(params.mixed(), false)));
                RandomNotesGenerator generator = RandomNotesGenerator.withSeed(settings, seed);
                yield new PracticePlan(targets(generator::nextMeasure, notes), timing, mode, seed,
                        target -> Optional.empty());
            }
            case "chords" -> chords(params, hands, range, keyboard, key, noteValue, timing, mode, seed, notes);
            default -> throw new IllegalArgumentException("Unknown exercise, expected random or chords: "
                    + params.exercise());
        };
    }

    private static PracticePlan chords(SessionStartParams params, Set<Hand> hands, KeyboardRange range,
                                       KeyboardRange keyboard, KeySignature key, NoteValue noteValue,
                                       SessionTiming timing, FollowMode mode, long seed, int notes) {
        Set<ChordType> types = or(params.chordTypes(), List.of("maj", "min")).stream()
                .map(ChordType::forCode)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ChordType.class)));
        Set<Inversion> inversions = or(params.inversions(), List.of(0)).stream()
                .map(Inversion::of)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Inversion.class)));
        RandomChordGenerator generator = RandomChordGenerator.withSeed(new ChordSettings(hands, range, keyboard, key,
                types, inversions, noteValue, timing.meter()), seed);
        Map<Fraction, Chord> chordsByStart = new ConcurrentHashMap<>();
        Supplier<List<NoteEvent>> measures = () -> {
            List<ChordEvent> measure = generator.nextMeasure();
            measure.forEach(event -> chordsByStart.put(event.start(), event.chord()));
            return measure.stream().flatMap(event -> event.notes().stream()).toList();
        };
        return new PracticePlan(targets(measures, notes), timing, mode, seed,
                target -> Optional.ofNullable(chordsByStart.remove(target.start())).map(chord -> chord.name(key)));
    }

    private static TargetSource targets(Supplier<List<NoteEvent>> measures, int notes) {
        TargetSource source = TargetSource.fromMeasures(measures);
        return notes > 0 ? source.limitedTo(notes) : source;
    }

    private static Set<Hand> hands(String hands) {
        return switch (hands.toLowerCase(Locale.ROOT)) {
            case "right" -> EnumSet.of(Hand.RIGHT);
            case "left" -> EnumSet.of(Hand.LEFT);
            case "both" -> EnumSet.allOf(Hand.class);
            default -> throw new IllegalArgumentException("Hands must be right, left or both: " + hands);
        };
    }

    private static KeyboardRange defaultRange(Set<Hand> hands) {
        if (hands.size() > 1) {
            return KeyboardRange.parse("C2-C6");
        }
        return hands.contains(Hand.RIGHT) ? KeyboardRange.parse("C4-C6") : KeyboardRange.parse("C2-C4");
    }

    private static <T> T or(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
