package bg.petarh.piano.model.time;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TimeSignatureTest {

    @ParameterizedTest
    @CsvSource({
            "2, 4, 1/2",
            "3, 4, 3/4",
            "4, 4, 1",
            "6, 8, 3/4",
            "2, 2, 1",
            "12, 8, 3/2"
    })
    void measureLengthIsBeatsTimesBeatUnit(int beats, int beatUnit, String length) {
        assertThat(TimeSignature.of(beats, beatUnit).measureLength().toString()).isEqualTo(length);
    }

    @ParameterizedTest
    @CsvSource({
            "6, 8, true",
            "9, 8, true",
            "12, 8, true",
            "6, 4, true",
            "3, 8, false",
            "3, 4, false",
            "4, 4, false",
            "2, 4, false"
    })
    void recognisesCompoundMeters(int beats, int beatUnit, boolean compound) {
        assertThat(TimeSignature.of(beats, beatUnit).isCompound()).isEqualTo(compound);
    }

    @Test
    void simpleMeterIsCountedInItsBeatUnit() {
        TimeSignature threeFour = TimeSignature.of(3, 4);

        assertThat(threeFour.countedBeat()).isEqualTo(Fraction.of(1, 4));
        assertThat(threeFour.beatsPerMeasure()).isEqualTo(3);
    }

    @Test
    void compoundMeterIsCountedInDottedBeats() {
        TimeSignature sixEight = TimeSignature.of(6, 8);

        assertThat(sixEight.countedBeat()).isEqualTo(Fraction.of(3, 8));
        assertThat(sixEight.beatsPerMeasure()).isEqualTo(2);
    }

    @Test
    void parsesNotation() {
        assertThat(TimeSignature.parse("6/8")).isEqualTo(TimeSignature.of(6, 8));
        assertThat(TimeSignature.parse("3/4").toString()).isEqualTo("3/4");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "4", "4/", "/4", "a/4", "4/4/4", "0/4"})
    void rejectsInvalidNotation(String notation) {
        assertThatIllegalArgumentException().isThrownBy(() -> TimeSignature.parse(notation));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 33})
    void rejectsBeatCountOutOfRange(int beats) {
        assertThatIllegalArgumentException().isThrownBy(() -> TimeSignature.of(beats, 4));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 3, 6, 128})
    void rejectsBeatUnitThatIsNotANoteValue(int beatUnit) {
        assertThatIllegalArgumentException().isThrownBy(() -> TimeSignature.of(4, beatUnit));
    }

    @Test
    void offersThePracticeMeters() {
        assertThat(TimeSignature.PRACTICE_METERS)
                .extracting(TimeSignature::toString)
                .containsExactly("2/4", "3/4", "4/4", "6/8");
    }

    @Test
    void commonTimeIsFourFour() {
        assertThat(TimeSignature.COMMON_TIME).isEqualTo(TimeSignature.of(4, 4));
    }
}
