package bg.petarh.piano.api.session;

import bg.petarh.piano.engine.evaluation.FollowMode;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.engine.songs.OutOfRange;
import bg.petarh.piano.engine.songs.SectionLoop;
import bg.petarh.piano.engine.songs.SongSection;
import bg.petarh.piano.engine.songs.SongSelection;
import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.generator.chords.ChordEvent;
import bg.petarh.piano.generator.chords.ChordSettings;
import bg.petarh.piano.generator.chords.RandomChordGenerator;
import bg.petarh.piano.generator.notes.Level;
import bg.petarh.piano.generator.notes.RandomNotesGenerator;
import bg.petarh.piano.generator.notes.RandomNotesOptions;
import bg.petarh.piano.generator.notes.RandomNotesSettings;
import bg.petarh.piano.library.settings.Settings;
import bg.petarh.piano.model.chord.Chord;
import bg.petarh.piano.model.chord.ChordType;
import bg.petarh.piano.model.chord.Inversion;
import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.note.Targets;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.NoteValue;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

final class PracticePlan {

    private static final int DEFAULT_TEMPO = 90;
    private static final int DEFAULT_NOTES = 50;
    private static final int DEFAULT_COUNT_IN_BARS = 1;
    private static final int FULL_TEMPO_PERCENT = 100;

    private final Settings settings;
    private final SessionTiming timing;
    private final FollowMode mode;
    private final long seed;
    private final KeySignature key;
    private final Map<Fraction, Chord> chordsByStart = new ConcurrentHashMap<>();
    private final SwitchingMeasures measures;
    private final TargetSource targets;
    private final Integer songTempo;
    private final List<String> notices;
    private final List<NoteEvent> accompaniment;
    private final List<NoteEvent> songNotes;
    private final SectionLoop loop;
    private final Fraction sectionStart;
    private volatile SessionStartParams params;

    private PracticePlan(SessionStartParams params, Settings settings, SessionTiming timing, FollowMode mode,
                         long seed) {
        this.params = params;
        this.settings = settings;
        this.timing = timing;
        this.mode = mode;
        this.seed = seed;
        this.key = KeySignature.parse(or(params.key(), "C"));
        this.measures = new SwitchingMeasures(measuresFor(params, seed, 0));
        TargetSource source = TargetSource.fromMeasures(measures);
        int notes = or(params.notes(), DEFAULT_NOTES);
        this.targets = notes > 0 ? source.limitedTo(notes) : source;
        this.songTempo = null;
        this.notices = List.of();
        this.accompaniment = List.of();
        this.songNotes = List.of();
        this.loop = null;
        this.sectionStart = null;
    }

    private PracticePlan(SessionStartParams params, Settings settings, SessionTiming timing, FollowMode mode,
                         Score song, SectionLoop loop, Fraction sectionStart, List<String> notices) {
        this.params = params;
        this.settings = settings;
        this.timing = timing;
        this.mode = mode;
        this.seed = 0;
        this.key = song.keyAt(Fraction.ZERO);
        this.measures = null;
        this.targets = TargetSource.of(Targets.from(loop.played()));
        this.songTempo = loop.tempoMap().tempoAt(Fraction.ZERO).bpm();
        this.notices = List.copyOf(notices);
        this.accompaniment = loop.accompaniment();
        List<NoteEvent> whole = new ArrayList<>(loop.played());
        whole.addAll(loop.accompaniment());
        this.songNotes = List.copyOf(whole);
        this.loop = loop;
        this.sectionStart = sectionStart;
    }

    static PracticePlan fromSong(SessionStartParams params, Settings settings, Score song) {
        if (params.input() == null) {
            throw new IllegalArgumentException("input is required: the MIDI input device id");
        }
        FollowMode mode = followMode(params.mode());
        int percent = or(params.tempoPercent(), FULL_TEMPO_PERCENT);
        if (percent < 1) {
            throw new IllegalArgumentException("tempoPercent must be positive: " + percent);
        }

        String partId = params.part() != null ? params.part()
                : song.parts().stream().findFirst().map(Part::id)
                .orElseThrow(() -> new IllegalArgumentException("The song has no parts"));
        KeyboardRange keyboard = params.keyboard() != null
                ? KeyboardRange.parseKeyboard(params.keyboard())
                : settings.keyboardFor(params.input()).orElse(KeyboardRange.fullPiano());
        OutOfRange outOfRange = "skip".equalsIgnoreCase(params.outOfRange()) ? OutOfRange.SKIP : OutOfRange.SHIFT_OCTAVE;
        SongSelection selection = SongSelection.of(song, partId, hands(or(params.hands(), "right")), keyboard, outOfRange);
        SongSection section = new SongSection(or(params.fromBar(), 1), or(params.toBar(), song.measureCount()));
        SectionLoop loop = section.loop(song, selection, or(params.repeats(), 1));
        Fraction sectionStart = song.measureStarts().get(section.firstBar() - 1);
        SessionTiming timing = new SessionTiming(loop.tempoMap().scaledBy(percent / 100.0),
                song.timeSignatureAt(sectionStart), or(params.countIn(), DEFAULT_COUNT_IN_BARS));
        List<String> notices = new ArrayList<>();
        if (selection.shifted() > 0) {
            notices.add(count(selection.shifted()) + " outside your keyboard " + was(selection.shifted())
                    + " moved by an octave");
        }
        if (selection.skipped() > 0) {
            notices.add(count(selection.skipped()) + " outside your keyboard " + was(selection.skipped()) + " skipped");
        }
        return new PracticePlan(params, settings, timing, mode, song, loop, sectionStart, notices);
    }

    private static String count(int notes) {
        return notes + (notes == 1 ? " note" : " notes");
    }

    private static String was(int notes) {
        return notes == 1 ? "was" : "were";
    }

    Optional<Fraction> sectionStart() {
        return Optional.ofNullable(sectionStart);
    }

    Optional<SectionLoop> loop() {
        return Optional.ofNullable(loop);
    }

    boolean isSong() {
        return measures == null;
    }

    Integer songTempo() {
        return songTempo;
    }

    List<String> notices() {
        return notices;
    }

    List<NoteEvent> playbackNotes() {
        return mode == FollowMode.LISTEN ? songNotes : accompaniment;
    }

    private static FollowMode followMode(String mode) {
        String name = or(mode, "wait").toUpperCase(Locale.ROOT).replace('-', '_');
        try {
            return FollowMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("mode must be wait, tempo, listen or play-along: " + mode);
        }
    }

    static PracticePlan from(SessionStartParams params, Settings settings) {
        if (params.input() == null) {
            throw new IllegalArgumentException("input is required: the MIDI input device id");
        }
        FollowMode mode = followMode(params.mode());
        int bpm = params.tempo() != null ? params.tempo() : settings.tempoFor(modeKey(mode)).orElse(DEFAULT_TEMPO);
        SessionTiming timing = new SessionTiming(Tempo.of(bpm),
                TimeSignature.parse(or(params.time(), "4/4")), or(params.countIn(), DEFAULT_COUNT_IN_BARS));
        long seed = params.seed() != null ? params.seed() : ThreadLocalRandom.current().nextLong();
        return new PracticePlan(params, settings, timing, mode, seed);
    }

    TargetSource targets() {
        return targets;
    }

    SessionTiming timing() {
        return timing;
    }

    FollowMode mode() {
        return mode;
    }

    long seed() {
        return seed;
    }

    String modeKey() {
        return modeKey(mode);
    }

    Optional<String> nameOf(Target target) {
        return Optional.ofNullable(chordsByStart.remove(target.start())).map(chord -> chord.name(key));
    }

    Consumer<Optional<Target>> adjust(AdjustParams change) {
        if (isSong()) {
            throw new IllegalArgumentException("A song's hands and notes can't be changed while playing; start it again");
        }
        SessionStartParams adjusted = params.adjustedBy(change);
        measuresFor(adjusted, seed, 0);
        return current -> {
            long nextMeasure = current
                    .map(target -> target.start().dividedBy(timing.meter().measureLength()).floor() + 1)
                    .orElse(0L);
            measures.switchTo(measuresFor(adjusted, seed + nextMeasure, nextMeasure));
            targets.discardLookAhead();
            params = adjusted;
        };
    }

    private Supplier<List<NoteEvent>> measuresFor(SessionStartParams source, long generatorSeed, long firstMeasure) {
        Set<Hand> hands = hands(or(source.hands(), "right"));
        KeyboardRange range = source.range() != null ? KeyboardRange.parse(source.range()) : defaultRange(hands);
        KeyboardRange keyboard = source.keyboard() != null
                ? KeyboardRange.parseKeyboard(source.keyboard())
                : settings.keyboardFor(source.input()).orElse(KeyboardRange.fullPiano());
        NoteValue noteValue = NoteValue.valueOf(or(source.noteValue(), "quarter").toUpperCase(Locale.ROOT));
        return switch (or(source.exercise(), "")) {
            case "random" -> RandomNotesGenerator.withSeed(new RandomNotesSettings(hands, range, keyboard, key,
                    noteValue, timing.meter(), Level.of(or(source.level(), 1)), RandomNotesOptions.defaults()
                    .withAccidentals(or(source.accidentals(), false))
                    .withMixedNoteValues(or(source.mixed(), false))), generatorSeed, firstMeasure)::nextMeasure;
            case "chords" -> chords(source, hands, range, keyboard, noteValue, generatorSeed, firstMeasure);
            default -> throw new IllegalArgumentException("Unknown exercise, expected random or chords: "
                    + source.exercise());
        };
    }

    private Supplier<List<NoteEvent>> chords(SessionStartParams source, Set<Hand> hands, KeyboardRange range,
                                             KeyboardRange keyboard, NoteValue noteValue, long generatorSeed,
                                             long firstMeasure) {
        Set<ChordType> types = or(source.chordTypes(), List.of("maj", "min")).stream()
                .map(ChordType::forCode)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ChordType.class)));
        Set<Inversion> inversions = or(source.inversions(), List.of(0)).stream()
                .map(Inversion::of)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Inversion.class)));
        RandomChordGenerator generator = RandomChordGenerator.withSeed(new ChordSettings(hands, range, keyboard, key,
                types, inversions, noteValue, timing.meter()), generatorSeed, firstMeasure);
        return () -> {
            List<ChordEvent> measure = generator.nextMeasure();
            measure.forEach(event -> chordsByStart.put(event.start(), event.chord()));
            return measure.stream().flatMap(event -> event.notes().stream()).toList();
        };
    }

    private static String modeKey(FollowMode mode) {
        return mode.name().toLowerCase(Locale.ROOT);
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
