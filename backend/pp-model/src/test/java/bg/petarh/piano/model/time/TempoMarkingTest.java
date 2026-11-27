package bg.petarh.piano.model.time;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class TempoMarkingTest {

    @ParameterizedTest
    @CsvSource({
            "GRAVE, 40",
            "LARGO, 50",
            "ADAGIO, 70",
            "ANDANTE, 90",
            "MODERATO, 110",
            "ALLEGRO, 130",
            "VIVACE, 160",
            "PRESTO, 180"
    })
    void offersStandardPresets(TempoMarking marking, int presetBpm) {
        assertThat(marking.preset()).isEqualTo(Tempo.of(presetBpm));
    }

    @ParameterizedTest
    @CsvSource({
            "20, GRAVE",
            "44, GRAVE",
            "45, LARGO",
            "60, ADAGIO",
            "79, ADAGIO",
            "80, ANDANTE",
            "100, MODERATO",
            "120, ALLEGRO",
            "145, VIVACE",
            "170, PRESTO",
            "240, PRESTO"
    })
    void namesTheMarkingForAnyBpm(int bpm, TempoMarking expected) {
        assertThat(TempoMarking.forBpm(bpm)).isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(TempoMarking.class)
    void presetBelongsToItsOwnMarking(TempoMarking marking) {
        assertThat(TempoMarking.forBpm(marking.preset().bpm())).isEqualTo(marking);
    }

    @Test
    void hasReadableNames() {
        assertThat(TempoMarking.MODERATO.displayName()).isEqualTo("Moderato");
    }
}
