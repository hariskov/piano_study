package bg.petarh.piano.model.chord;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ChordTest {

    @ParameterizedTest
    @CsvSource({
            "C4, MAJOR, C4 E4 G4",
            "C4, MINOR, C4 D#4 G4",
            "B3, DIMINISHED, B3 D4 F4",
            "C4, AUGMENTED, C4 E4 G#4",
            "C4, MAJOR_SEVENTH, C4 E4 G4 B4",
            "D4, MINOR_SEVENTH, D4 F4 A4 C5",
            "G3, DOMINANT_SEVENTH, G3 B3 D4 F4"
    })
    void stacksTheChordTypeOnTheRoot(String root, ChordType type, String pitches) {
        assertThat(names(Chord.of(Pitch.parse(root), type, Inversion.ROOT_POSITION))).isEqualTo(pitches);
    }

    @ParameterizedTest
    @CsvSource({
            "FIRST, E4 G4 C5",
            "SECOND, G4 C5 E5"
    })
    void invertsByMovingTheLowestNotesUpAnOctave(Inversion inversion, String pitches) {
        assertThat(names(Chord.of(Pitch.parse("C4"), ChordType.MAJOR, inversion))).isEqualTo(pitches);
    }

    @Test
    void seventhChordsHaveAThirdInversion() {
        Chord chord = Chord.of(Pitch.parse("G3"), ChordType.DOMINANT_SEVENTH, Inversion.THIRD);

        assertThat(names(chord)).isEqualTo("F4 G4 B4 D5");
        assertThat(chord.bass()).isEqualTo(Pitch.parse("F4"));
    }

    @Test
    void triadsHaveNoThirdInversion() {
        assertThat(ChordType.MAJOR.allows(Inversion.THIRD)).isFalse();
        assertThat(ChordType.MINOR_SEVENTH.allows(Inversion.THIRD)).isTrue();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Chord.of(Pitch.parse("C4"), ChordType.MAJOR, Inversion.THIRD));
    }

    @Test
    void rejectsChordAboveTheHighestMidiNote() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Chord.of(Pitch.of(120), ChordType.MAJOR, Inversion.SECOND));
    }

    @ParameterizedTest
    @CsvSource({
            "C4, MAJOR, ROOT_POSITION, C, C",
            "D4, MINOR, FIRST, C, Dm/F",
            "B3, DIMINISHED, ROOT_POSITION, C, Bdim",
            "C4, AUGMENTED, ROOT_POSITION, C, Caug",
            "F4, MAJOR_SEVENTH, SECOND, C, Fmaj7/C",
            "G3, DOMINANT_SEVENTH, THIRD, C, G7/F",
            "A3, MINOR_SEVENTH, ROOT_POSITION, C, Am7",
            "A#3, MAJOR, ROOT_POSITION, F, Bb",
            "D#4, MAJOR, FIRST, Bb, Eb/G",
            "F#4, MINOR, SECOND, D, F#m/C#",
            "B3, MAJOR, FIRST, E, B/D#",
            "G#3, MAJOR, FIRST, Eb, Ab/C",
            "E4, DOMINANT_SEVENTH, FIRST, C, E7/G#",
            "G#4, AUGMENTED, SECOND, A, G#aug/D##"
    })
    void namesTheChordWithTheBassAfterASlash(String root, ChordType type, Inversion inversion,
                                            String keyTonic, String name) {
        Chord chord = Chord.of(Pitch.parse(root), type, inversion);

        assertThat(chord.name(KeySignature.major(keyTonic))).isEqualTo(name);
    }

    @Test
    void describesItself() {
        assertThat(Chord.of(Pitch.parse("D4"), ChordType.MINOR, Inversion.FIRST))
                .hasToString("Dm/F [F4, A4, D5]");
    }

    @Test
    void equalChordsHaveTheSameRootTypeAndInversion() {
        assertThat(Chord.of(Pitch.parse("C4"), ChordType.MAJOR, Inversion.FIRST))
                .isEqualTo(Chord.of(Pitch.parse("C4"), ChordType.MAJOR, Inversion.FIRST))
                .isNotEqualTo(Chord.of(Pitch.parse("C4"), ChordType.MAJOR, Inversion.SECOND));
    }

    private static String names(Chord chord) {
        return String.join(" ", chord.pitches().stream().map(Pitch::name).toList());
    }
}
