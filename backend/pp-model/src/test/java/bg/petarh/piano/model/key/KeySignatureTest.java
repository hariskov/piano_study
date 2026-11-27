package bg.petarh.piano.model.key;

import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class KeySignatureTest {

    @ParameterizedTest
    @CsvSource({
            "-7, Cb",
            "-3, Eb",
            "-1, F",
            "0, C",
            "1, G",
            "4, E",
            "6, F#",
            "7, C#"
    })
    void namesMajorTonicFromFifths(int fifths, String tonic) {
        assertThat(KeySignature.of(fifths, KeyMode.MAJOR).tonicName()).isEqualTo(tonic);
    }

    @ParameterizedTest
    @CsvSource({
            "-7, Ab",
            "-2, G",
            "0, A",
            "1, E",
            "3, F#",
            "7, A#"
    })
    void namesMinorTonicFromFifths(int fifths, String tonic) {
        assertThat(KeySignature.of(fifths, KeyMode.MINOR).tonicName()).isEqualTo(tonic);
    }

    @Test
    void createsKeysFromTonicNames() {
        assertThat(KeySignature.major("D")).isEqualTo(KeySignature.of(2, KeyMode.MAJOR));
        assertThat(KeySignature.major("Bb")).isEqualTo(KeySignature.of(-2, KeyMode.MAJOR));
        assertThat(KeySignature.minor("C#")).isEqualTo(KeySignature.of(4, KeyMode.MINOR));
        assertThat(KeySignature.minor("d")).isEqualTo(KeySignature.of(-1, KeyMode.MINOR));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "H", "Fb", "Db#", "C##"})
    void rejectsTonicWithoutAMajorKey(String tonic) {
        assertThatIllegalArgumentException().isThrownBy(() -> KeySignature.major(tonic));
    }

    @Test
    void rejectsTonicWithoutAMinorKey() {
        assertThatIllegalArgumentException().isThrownBy(() -> KeySignature.minor("Db"));
    }

    @ParameterizedTest
    @ValueSource(ints = {-8, 8})
    void rejectsMoreThanSevenAccidentals(int fifths) {
        assertThatIllegalArgumentException().isThrownBy(() -> KeySignature.of(fifths, KeyMode.MAJOR));
    }

    @Test
    void cMajorContainsOnlyWhiteKeys() {
        KeySignature cMajor = KeySignature.major("C");

        assertThat(KeyboardRange.of(Pitch.parse("C4"), Pitch.parse("B4")).pitches().stream().filter(cMajor::contains))
                .extracting(Pitch::name)
                .containsExactly("C4", "D4", "E4", "F4", "G4", "A4", "B4");
    }

    @Test
    void containsScaleNotesInEveryOctave() {
        KeySignature dMajor = KeySignature.major("D");

        assertThat(List.of("F#1", "C#4", "D7")).map(Pitch::parse).allMatch(dMajor::contains);
        assertThat(List.of("F1", "C4", "G#7")).map(Pitch::parse).noneMatch(dMajor::contains);
    }

    @Test
    void relativeMinorHasTheSameNotesAsItsMajor() {
        KeySignature eMinor = KeySignature.minor("E");
        KeySignature gMajor = KeySignature.major("G");

        assertThat(eMinor.pitchClasses()).isEqualTo(gMajor.pitchClasses());
        assertThat(eMinor.fifths()).isEqualTo(gMajor.fifths());
    }

    @Test
    void flatKeysUseFlatsAndSharpKeysUseSharps() {
        assertThat(KeySignature.major("F").usesFlats()).isTrue();
        assertThat(KeySignature.major("G").usesFlats()).isFalse();
        assertThat(KeySignature.major("C").usesFlats()).isFalse();
    }

    @Test
    void tonicPitchClassFollowsTheMode() {
        assertThat(KeySignature.major("Eb").tonicPitchClass()).isEqualTo(3);
        assertThat(KeySignature.minor("B").tonicPitchClass()).isEqualTo(11);
    }

    @Test
    void describesItself() {
        assertThat(KeySignature.major("Ab")).hasToString("Ab major");
        assertThat(KeySignature.minor("F#")).hasToString("F# minor");
    }
}
