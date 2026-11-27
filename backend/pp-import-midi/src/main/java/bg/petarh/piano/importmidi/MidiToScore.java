package bg.petarh.piano.importmidi;

import bg.petarh.piano.model.key.KeyMode;
import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.score.ScoreNote;
import bg.petarh.piano.model.score.Spelling;
import bg.petarh.piano.model.score.Tie;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class MidiToScore {

    private static final long MICROS_PER_MINUTE = 60_000_000L;

    private record Voice(int number, Fraction chordStart, Fraction end) {
    }

    public MidiToScore() {
    }

    public MidiConversion convert(RawMidi raw, MidiImportOptions options, String fileTitle) {
        Set<String> warnings = new LinkedHashSet<>(raw.warnings());
        Quantiser quantiser = new Quantiser(raw.ticksPerQuarter(), options);
        TreeMap<Fraction, TimeSignature> times = new TreeMap<>();
        for (RawTimeSignature time : raw.timeSignatures()) {
            try {
                times.put(quantiser.position(time.tick()), TimeSignature.of(time.numerator(), time.denominator()));
            } catch (IllegalArgumentException e) {
                warnings.add("The time signature " + time.numerator() + "/" + time.denominator() + " was ignored");
            }
        }
        TreeMap<Fraction, KeySignature> keys = new TreeMap<>();
        for (RawKeySignature key : raw.keySignatures()) {
            try {
                keys.put(quantiser.position(key.tick()), KeySignature.of(key.fifths(), key.minor() ? KeyMode.MINOR : KeyMode.MAJOR));
            } catch (IllegalArgumentException e) {
                warnings.add("A key signature with " + key.fifths() + " sharps or flats was ignored");
            }
        }
        TreeMap<Fraction, Tempo> tempos = new TreeMap<>();
        for (RawTempo tempo : raw.tempos()) {
            tempos.put(quantiser.position(tempo.tick()), tempo(tempo.microsPerQuarter(), warnings));
        }

        Map<RawMidi.TrackChannel, List<RawNote>> byPart = raw.notes().stream()
                .collect(Collectors.groupingBy(note -> new RawMidi.TrackChannel(note.track(), note.channel()),
                        () -> new TreeMap<>(Comparator.comparingInt(RawMidi.TrackChannel::track)
                                .thenComparingInt(RawMidi.TrackChannel::channel)),
                        Collectors.toList()));
        List<Part> parts = new ArrayList<>();
        Fraction lastEnd = Fraction.ZERO;
        for (Map.Entry<RawMidi.TrackChannel, List<RawNote>> part : byPart.entrySet()) {
            if (part.getKey().channel() == GeneralMidi.PERCUSSION_CHANNEL) {
                warnings.add("Percussion (channel 10) can't be played on the piano and was left out");
                continue;
            }
            List<Quantiser.Span> placed = part.getValue().stream()
                    .map(note -> quantiser.place(note.pitch(), note.startTick(), note.endTick())).toList();
            for (Quantiser.Span note : placed) {
                lastEnd = note.end().compareTo(lastEnd) > 0 ? note.end() : lastEnd;
            }
            parts.add(new Part("P" + (parts.size() + 1), name(raw, part.getKey()), 1, voiced(placed, keys)));
        }

        Fraction length = roundedUpToMeasure(lastEnd, times);
        Score.Builder score = Score.builder(raw.trackName(0).filter(name -> !name.isBlank()).orElse(fileTitle));
        parts.forEach(score::part);
        times.forEach((at, time) -> inside(at, length, () -> score.timeSignature(at, time)));
        keys.forEach((at, key) -> inside(at, length, () -> score.key(at, key)));
        tempos.forEach((at, tempo) -> inside(at, length, () -> score.tempo(at, tempo)));
        return new MidiConversion(score.length(length).build(), new ArrayList<>(warnings));
    }

    private static List<ScoreNote> voiced(List<Quantiser.Span> notes, TreeMap<Fraction, KeySignature> keys) {
        List<Quantiser.Span> ordered = notes.stream()
                .sorted(Comparator.comparing(Quantiser.Span::start).thenComparing(Quantiser.Span::pitch, Comparator.reverseOrder()))
                .toList();
        List<Voice> voices = new ArrayList<>();
        List<ScoreNote> scored = new ArrayList<>();
        for (Quantiser.Span note : ordered) {
            int voice = chooseVoice(voices, note);
            Map.Entry<Fraction, KeySignature> key = keys.floorEntry(note.start());
            boolean flats = key != null && key.getValue().usesFlats();
            scored.add(new ScoreNote(Spelling.of(Pitch.of(note.pitch()), flats), note.start(),
                    note.end().minus(note.start()), 1, voice, Tie.NONE));
        }
        return scored;
    }

    private static int chooseVoice(List<Voice> voices, Quantiser.Span note) {
        for (int index = 0; index < voices.size(); index++) {
            Voice voice = voices.get(index);
            boolean sameChord = voice.chordStart().equals(note.start()) && voice.end().equals(note.end());
            if (sameChord) {
                return voice.number();
            }
            if (voice.end().compareTo(note.start()) <= 0) {
                voices.set(index, new Voice(voice.number(), note.start(), note.end()));
                return voice.number();
            }
        }
        Voice added = new Voice(voices.size() + 1, note.start(), note.end());
        voices.add(added);
        return added.number();
    }

    private static Fraction roundedUpToMeasure(Fraction end, TreeMap<Fraction, TimeSignature> times) {
        Fraction at = Fraction.ZERO;
        while (at.compareTo(end) < 0) {
            Map.Entry<Fraction, TimeSignature> time = times.floorEntry(at);
            at = at.plus((time == null ? TimeSignature.COMMON_TIME : time.getValue()).measureLength());
        }
        return at.isZero() ? TimeSignature.COMMON_TIME.measureLength() : at;
    }

    private static void inside(Fraction at, Fraction length, Runnable add) {
        if (at.isZero() || at.compareTo(length) < 0) {
            add.run();
        }
    }

    private static Tempo tempo(int microsPerQuarter, Set<String> warnings) {
        long bpm = Math.round((double) MICROS_PER_MINUTE / microsPerQuarter);
        int clamped = Math.clamp(bpm, Tempo.SLOWEST_BPM, Tempo.FASTEST_BPM);
        if (clamped != bpm) {
            warnings.add("A tempo of " + bpm + " BPM was changed to " + clamped + " BPM, the closest that can be played");
        }
        return Tempo.of(clamped);
    }

    private static String name(RawMidi raw, RawMidi.TrackChannel part) {
        return raw.trackName(part.track()).filter(name -> !name.isBlank())
                .or(() -> raw.program(part.track(), part.channel()).map(GeneralMidi::instrument))
                .orElse("Track " + part.track());
    }
}
