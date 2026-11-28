package bg.petarh.piano.importmidi;

import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartChoiceTest {

    private static final int PPQ = 480;
    private static final RawMidi HANDS_AND_FLUTE = midi(
            List.of(note(1, 76, 0), note(1, 77, 7), note(2, 45, 0), note(3, 67, 0)),
            Map.of(1, "Piano RH", 2, "Piano LH", 3, "Melody"),
            Map.of(new RawMidi.TrackChannel(3, 0), 73));

    private final MidiToScore converter = new MidiToScore();

    @Test
    void namesTheInstrumentOfEveryPart() {
        MidiConversion converted = converter.convert(HANDS_AND_FLUTE, MidiImportOptions.defaults(), "song");

        assertThat(converted.instruments()).containsExactly(Map.entry("P1", "Acoustic Grand Piano"), Map.entry("P2", "Flute"));
    }

    @Test
    void leavesOutTheExcludedPartsAndKeepsTheOthersIds() {
        MidiImportOptions options = MidiImportOptions.defaults().withExcludedParts(Set.of("P1"));

        MidiConversion converted = converter.convert(HANDS_AND_FLUTE, options, "song");

        assertThat(converted.score().parts()).extracting(Part::id, Part::name).containsExactly(tuple("P2", "Melody"));
        assertThat(converted.instruments()).containsExactly(Map.entry("P2", "Flute"));
    }

    @Test
    void endsTheSongWithThePartsKept() {
        MidiImportOptions options = MidiImportOptions.defaults().withExcludedParts(Set.of("P1"));

        MidiConversion converted = converter.convert(HANDS_AND_FLUTE, options, "song");

        assertThat(converted.score().length()).isEqualTo(Fraction.whole(1));
    }

    @Test
    void refusesToLeaveOutEveryPart() {
        MidiImportOptions options = MidiImportOptions.defaults().withExcludedParts(Set.of("P1", "P2"));

        assertThatThrownBy(() -> converter.convert(HANDS_AND_FLUTE, options, "song"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Choose at least one part to import");
    }

    private static RawMidi midi(List<RawNote> notes, Map<Integer, String> names, Map<RawMidi.TrackChannel, Integer> programs) {
        long end = notes.stream().mapToLong(RawNote::endTick).max().orElse(0);
        return new RawMidi(1, PPQ, notes, names, programs, List.of(), List.of(), List.of(), end, List.of());
    }

    private static RawNote note(int track, int pitch, int beat) {
        return new RawNote(track, 0, pitch, 80, (long) beat * PPQ, (long) (beat + 1) * PPQ);
    }
}
