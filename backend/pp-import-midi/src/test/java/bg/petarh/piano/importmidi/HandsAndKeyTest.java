package bg.petarh.piano.importmidi;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HandsAndKeyTest {

    private static final int PPQ = 480;

    private final MidiToScore converter = new MidiToScore();

    @Test
    void splitsAOneTrackPianoPartAtMiddleC() {
        Part piano = convert(midi(List.of(note(1, 72, 0), note(1, 48, 0)), Map.of(), Map.of()))
                .score().parts().getFirst();

        assertThat(piano.staffCount()).isEqualTo(2);
        assertThat(piano.playableNotes()).extracting(note -> note.pitch().name() + " " + note.hand())
                .containsExactly("C3 LEFT", "C5 RIGHT");
    }

    @Test
    void splitsAtAChosenPoint() {
        RawMidi raw = midi(List.of(note(1, 57, 0), note(1, 52, 0)), Map.of(), Map.of());

        Part piano = converter.convert(raw, MidiImportOptions.defaults().withSplitPoint(55), "x").score().parts().getFirst();

        assertThat(piano.playableNotes()).extracting(NoteEvent::hand).containsExactly(Hand.LEFT, Hand.RIGHT);
    }

    @Test
    void joinsTracksNamedForEachHandIntoOnePart() {
        RawMidi raw = midi(List.of(note(1, 76, 0), note(2, 45, 0), note(3, 67, 0)),
                Map.of(1, "Piano RH", 2, "Piano LH", 3, "Flute"),
                Map.of(new RawMidi.TrackChannel(3, 0), 73));

        Score score = convert(raw).score();

        assertThat(score.parts()).extracting(Part::name).containsExactly("Piano", "Flute");
        Part piano = score.parts().getFirst();
        assertThat(piano.staffCount()).isEqualTo(2);
        assertThat(piano.notes()).extracting(note -> note.spelling() + " staff " + note.staff())
                .containsExactlyInAnyOrder("E5 staff 1", "A2 staff 2");
    }

    @Test
    void joinsEveryVoiceTrackNamedForAHandIntoOnePart() {
        RawMidi raw = midi(List.of(note(1, 76, 0), note(2, 72, 0), note(3, 48, 0), note(4, 43, 0)),
                Map.of(1, "up:", 2, "upII:", 3, "down:", 4, "downII:"), Map.of());

        List<Part> parts = convert(raw).score().parts();

        assertThat(parts).hasSize(1);
        assertThat(parts.getFirst().notes()).extracting(note -> note.pitch().midiNumber() + " staff " + note.staff())
                .containsExactlyInAnyOrder("76 staff 1", "72 staff 1", "48 staff 2", "43 staff 2");
    }

    @Test
    void joinsTheOnlyTwoPianoTracksWhenTheyLieApart() {
        RawMidi raw = midi(List.of(note(1, 50, 0), note(1, 52, 1), note(2, 72, 0), note(2, 74, 1)), Map.of(), Map.of());

        Part piano = convert(raw).score().parts().getFirst();

        assertThat(convert(raw).score().parts()).hasSize(1);
        assertThat(piano.notes()).filteredOn(note -> note.staff() == 1).extracting(note -> note.pitch().midiNumber())
                .containsExactlyInAnyOrder(72, 74);
    }

    @Test
    void keepsTwoPianoTracksInTheSameRangeApart() {
        RawMidi raw = midi(List.of(note(1, 60, 0), note(1, 64, 1), note(2, 62, 0), note(2, 65, 1)), Map.of(), Map.of());

        assertThat(convert(raw).score().parts()).hasSize(2);
    }

    @Test
    void keepsOtherInstrumentsOnOneStaff() {
        RawMidi raw = midi(List.of(note(1, 76, 0), note(1, 48, 1)), Map.of(), Map.of(new RawMidi.TrackChannel(1, 0), 40));

        assertThat(convert(raw).score().parts().getFirst().staffCount()).isEqualTo(1);
    }

    @Test
    void estimatesTheKeyWhenTheFileHasNone() {
        MidiConversion gMajor = convert(midi(scale(new int[]{67, 69, 71, 72, 74, 76, 78, 79, 74, 71, 67}),
                Map.of(), Map.of()));

        assertThat(gMajor.score().keyAt(Fraction.ZERO)).isEqualTo(KeySignature.major("G"));
        assertThat(gMajor.warnings()).anySatisfy(warning -> assertThat(warning).contains("estimated").contains("G major"));
        assertThat(gMajor.score().parts().getFirst().notes()).extracting(note -> note.spelling().toString())
                .contains("F#5");
    }

    @Test
    void estimatesMinorKeys() {
        MidiConversion aMinor = convert(midi(scale(new int[]{57, 60, 64, 69, 64, 60, 57, 59, 60, 62, 64, 65, 68, 69, 57}),
                Map.of(), Map.of()));

        assertThat(aMinor.score().keyAt(Fraction.ZERO)).isEqualTo(KeySignature.minor("A"));
    }

    @Test
    void trustsTheKeyInTheFile() {
        RawMidi raw = new RawMidi(1, PPQ, scale(new int[]{67, 69, 71, 72, 74, 76, 78, 79}), Map.of(), Map.of(), List.of(),
                List.of(), List.of(new RawKeySignature(0, 0, false)), 8L * PPQ, List.of());

        MidiConversion result = convert(raw);

        assertThat(result.score().keyAt(Fraction.ZERO)).isEqualTo(KeySignature.major("C"));
        assertThat(result.warnings()).noneSatisfy(warning -> assertThat(warning).contains("estimated"));
    }

    private MidiConversion convert(RawMidi raw) {
        return converter.convert(raw, MidiImportOptions.defaults(), "song");
    }

    private static List<RawNote> scale(int[] pitches) {
        List<RawNote> notes = new ArrayList<>();
        for (int i = 0; i < pitches.length; i++) {
            notes.add(note(1, pitches[i], i));
        }
        return notes;
    }

    private static RawMidi midi(List<RawNote> notes, Map<Integer, String> names, Map<RawMidi.TrackChannel, Integer> programs) {
        long end = notes.stream().mapToLong(RawNote::endTick).max().orElse(0);
        return new RawMidi(1, PPQ, notes, names, programs, List.of(), List.of(), List.of(), end, List.of());
    }

    private static RawNote note(int track, int pitch, int beat) {
        return new RawNote(track, 0, pitch, 80, (long) beat * PPQ, (long) (beat + 1) * PPQ);
    }
}
