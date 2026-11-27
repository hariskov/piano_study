package bg.petarh.piano.model.time;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class NoteValueTest {

    @ParameterizedTest
    @CsvSource({
            "WHOLE, 1",
            "HALF, 1/2",
            "QUARTER, 1/4",
            "EIGHTH, 1/8"
    })
    void lastsItsFractionOfAWholeNote(NoteValue value, String duration) {
        assertThat(value.duration().toString()).isEqualTo(duration);
    }

    @ParameterizedTest
    @CsvSource({
            "QUARTER, 4/4, true",
            "WHOLE, 4/4, true",
            "QUARTER, 3/4, true",
            "EIGHTH, 6/8, true",
            "QUARTER, 6/8, true",
            "HALF, 3/4, false",
            "WHOLE, 2/4, false",
            "HALF, 6/8, false"
    })
    void fillsAMeasureOnlyWithAWholeNumberOfNotes(NoteValue value, String meter, boolean fills) {
        assertThat(value.fillsMeasureOf(TimeSignature.parse(meter))).isEqualTo(fills);
    }

    @ParameterizedTest
    @CsvSource({
            "QUARTER, 4/4, 4",
            "EIGHTH, 6/8, 6",
            "WHOLE, 4/4, 1"
    })
    void countsNotesPerMeasure(NoteValue value, String meter, int count) {
        assertThat(value.notesPerMeasure(TimeSignature.parse(meter))).isEqualTo(count);
    }
}
