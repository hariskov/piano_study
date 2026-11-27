package bg.petarh.piano.importmidi;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.score.ScoreNote;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class MidiToScoreTest {

    private static final int PPQ = 480;

    private final MidiToScore converter = new MidiToScore();

    @Test
    void snapsNotesToTheSixteenthGrid() {
        MidiConversion result = convert(midi(List.of(
                note(1, 0, 60, 7, 470),
                note(1, 0, 62, 485, 600))));

        assertThat(notes(result, 0)).extracting(note -> note.start() + "+" + note.duration())
                .containsExactly("0+1/4", "1/4+1/16");
    }

    @Test
    void givesNotesShorterThanTheGridOneGridStep() {
        MidiConversion result = convert(midi(List.of(note(1, 0, 60, 0, 10))));

        assertThat(notes(result, 0).getFirst().duration()).isEqualTo(Fraction.of(1, 16));
    }

    @Test
    void snapsToTripletsOnlyWhenAsked() {
        RawMidi triplets = midi(List.of(note(1, 0, 60, 0, 160), note(1, 0, 62, 160, 320), note(1, 0, 64, 320, 480)));

        assertThat(notes(convert(triplets), 0)).extracting(ScoreNote::start)
                .containsExactly(Fraction.ZERO, Fraction.of(1, 16), Fraction.of(3, 16));
        assertThat(notes(converter.convert(triplets, MidiImportOptions.defaults().withTriplets(true), "x"), 0))
                .extracting(ScoreNote::start)
                .containsExactly(Fraction.ZERO, Fraction.of(1, 12), Fraction.of(1, 6));
    }

    @Test
    void makesOnePartPerTrackAndChannelNamedByTrackOrInstrument() {
        RawMidi raw = midi(List.of(note(1, 0, 60, 0, PPQ), note(2, 4, 67, 0, PPQ), note(3, 1, 50, 0, PPQ)),
                Map.of(1, "Melody"), Map.of(new RawMidi.TrackChannel(2, 4), 40));

        Score score = convert(raw).score();

        assertThat(score.parts()).extracting(Part::id, Part::name).containsExactly(
                tuple("P1", "Melody"),
                tuple("P2", "Violin"),
                tuple("P3", "Acoustic Grand Piano"));
    }

    @Test
    void leavesOutPercussionAndSaysSo() {
        MidiConversion result = convert(midi(List.of(note(1, 0, 60, 0, PPQ), note(1, 9, 36, 0, PPQ))));

        assertThat(result.score().parts()).hasSize(1);
        assertThat(result.warnings()).anySatisfy(warning -> assertThat(warning).contains("Percussion"));
    }

    @Test
    void carriesOverTempoTimeAndKeySignatures() {
        RawMidi raw = new RawMidi(1, PPQ, List.of(note(1, 0, 60, 0, 8L * PPQ)), Map.of(), Map.of(),
                List.of(new RawTempo(0, 750_000), new RawTempo(3L * PPQ, 400_000)),
                List.of(new RawTimeSignature(0, 3, 4)),
                List.of(new RawKeySignature(0, -1, false)), 8L * PPQ, List.of());

        Score score = convert(raw).score();

        assertThat(score.tempoMap().tempoAt(Fraction.ZERO)).isEqualTo(Tempo.of(80));
        assertThat(score.tempoMap().tempoAt(Fraction.of(3, 4))).isEqualTo(Tempo.of(150));
        assertThat(score.timeSignatureAt(Fraction.ZERO)).isEqualTo(TimeSignature.parse("3/4"));
        assertThat(score.keyAt(Fraction.ZERO)).isEqualTo(KeySignature.major("F"));
    }

    @Test
    void keepsTempoWithinWhatCanBePlayed() {
        RawMidi raw = new RawMidi(1, PPQ, List.of(note(1, 0, 60, 0, PPQ)), Map.of(), Map.of(),
                List.of(new RawTempo(0, 100_000)), List.of(), List.of(), PPQ, List.of());

        MidiConversion result = convert(raw);

        assertThat(result.score().tempoMap().tempoAt(Fraction.ZERO)).isEqualTo(Tempo.of(240));
        assertThat(result.warnings()).anySatisfy(warning -> assertThat(warning).contains("600 BPM"));
    }

    @Test
    void putsChordsInOneVoiceAndOverlappingNotesInTheirOwn() {
        MidiConversion result = convert(midi(List.of(
                note(1, 0, 60, 0, 2L * PPQ),
                note(1, 0, 64, 0, 2L * PPQ),
                note(1, 0, 72, 0, PPQ),
                note(1, 0, 74, PPQ, 2L * PPQ))));

        assertThat(notes(result, 0)).extracting(note -> note.spelling() + " voice " + note.voice()).containsExactlyInAnyOrder(
                "C4 voice 2", "E4 voice 2", "C5 voice 1", "D5 voice 1");
    }

    @Test
    void spellsNotesAsTheKeyPrefers() {
        RawMidi flatKey = new RawMidi(1, PPQ, List.of(note(1, 0, 70, 0, PPQ)), Map.of(), Map.of(), List.of(),
                List.of(), List.of(new RawKeySignature(0, -2, false)), PPQ, List.of());

        assertThat(notes(convert(flatKey), 0).getFirst().spelling()).hasToString("Bb4");
        RawMidi sharpKey = new RawMidi(1, PPQ, List.of(note(1, 0, 70, 0, PPQ)), Map.of(), Map.of(), List.of(),
                List.of(), List.of(new RawKeySignature(0, 0, false)), PPQ, List.of());
        assertThat(notes(convert(sharpKey), 0).getFirst().spelling()).hasToString("A#4");
    }

    @Test
    void roundsTheSongUpToAWholeMeasure() {
        Score score = convert(midi(List.of(note(1, 0, 60, 0, 5L * PPQ)))).score();

        assertThat(score.length()).isEqualTo(Fraction.whole(2));
    }

    @Test
    void namesTheSongAfterTheConductorTrackOrTheFile() {
        RawMidi named = midi(List.of(note(1, 0, 60, 0, PPQ)), Map.of(0, "Sonatina"), Map.of());

        assertThat(converter.convert(named, MidiImportOptions.defaults(), "file").score().title()).isEqualTo("Sonatina");
        assertThat(converter.convert(midi(List.of(note(1, 0, 60, 0, PPQ))), MidiImportOptions.defaults(), "file")
                .score().title()).isEqualTo("file");
    }

    @Test
    void givesTheSameScoreEveryTime() {
        List<RawNote> notes = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            notes.add(note(1 + i % 2, 0, 50 + (i * 7) % 30, i * 113L, i * 113L + 300));
        }
        RawMidi raw = midi(notes);

        assertThat(convert(raw).score().parts()).isEqualTo(convert(raw).score().parts());
    }

    private MidiConversion convert(RawMidi raw) {
        return converter.convert(raw, MidiImportOptions.defaults(), "song");
    }

    private static List<ScoreNote> notes(MidiConversion result, int part) {
        return result.score().parts().get(part).notes();
    }

    private static RawMidi midi(List<RawNote> notes) {
        return midi(notes, Map.of(), Map.of());
    }

    private static RawMidi midi(List<RawNote> notes, Map<Integer, String> names, Map<RawMidi.TrackChannel, Integer> programs) {
        long end = notes.stream().mapToLong(RawNote::endTick).max().orElse(0);
        return new RawMidi(1, PPQ, notes, names, programs, List.of(), List.of(), List.of(), end, List.of());
    }

    private static RawNote note(int track, int channel, int pitch, long start, long end) {
        return new RawNote(track, channel, pitch, 80, start, end);
    }
}
