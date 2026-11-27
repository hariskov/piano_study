package io.github.hariskov.piano.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PitchTest {

    @ParameterizedTest
    @CsvSource({
            "0, C-1",
            "21, A0",
            "60, C4",
            "61, C#4",
            "69, A4",
            "108, C8",
            "127, G9"
    })
    void namesPitchWithSharpSpellingAndScientificOctave(int midiNumber, String expectedName) {
        assertThat(Pitch.of(midiNumber).name()).isEqualTo(expectedName);
    }

    @ParameterizedTest
    @CsvSource({
            "C4, 60",
            "c4, 60",
            "C#4, 61",
            "Db4, 61",
            "B#3, 60",
            "Cb4, 59",
            "A0, 21",
            "C-1, 0",
            "G9, 127"
    })
    void parsesNoteNames(String name, int expectedMidiNumber) {
        assertThat(Pitch.parse(name).midiNumber()).isEqualTo(expectedMidiNumber);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 128})
    void rejectsMidiNumbersOutsideMidiRange(int midiNumber) {
        assertThatIllegalArgumentException().isThrownBy(() -> Pitch.of(midiNumber));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "H4", "C", "C#", "4", "C10", "Cb-1", "G#9", "C##4"})
    void rejectsInvalidNoteNames(String name) {
        assertThatIllegalArgumentException().isThrownBy(() -> Pitch.parse(name));
    }

    @ParameterizedTest
    @CsvSource({
            "C4, false",
            "C#4, true",
            "E4, false",
            "F#4, true",
            "A#0, true",
            "B4, false"
    })
    void identifiesBlackKeys(String name, boolean expectedBlack) {
        assertThat(Pitch.parse(name).isBlackKey()).isEqualTo(expectedBlack);
    }

    @Test
    void ordersByMidiNumber() {
        assertThat(Pitch.parse("C4")).isLessThan(Pitch.parse("C#4"));
        assertThat(Pitch.parse("C5")).isGreaterThan(Pitch.parse("B4"));
    }

    @Test
    void transposesBySemitones() {
        assertThat(Pitch.parse("C4").transpose(12)).isEqualTo(Pitch.parse("C5"));
        assertThat(Pitch.parse("C4").transpose(-1)).isEqualTo(Pitch.parse("B3"));
    }
}
