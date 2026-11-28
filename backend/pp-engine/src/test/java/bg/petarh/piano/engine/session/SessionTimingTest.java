package bg.petarh.piano.engine.session;

import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TempoMap;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SessionTimingTest {

    @ParameterizedTest
    @CsvSource({
            "120, 4/4, 0, 0",
            "120, 4/4, 1, 2000000000",
            "60, 3/4, 2, 6000000000",
            "90, 6/8, 1, 2000000000"
    })
    void countInLastsTheChosenNumberOfBars(int bpm, String meter, int bars, long nanos) {
        SessionTiming timing = new SessionTiming(Tempo.of(bpm), TimeSignature.parse(meter), bars);

        assertThat(timing.countInNanos()).isEqualTo(nanos);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 3})
    void countInIsOffOneOrTwoBars(int bars) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new SessionTiming(Tempo.of(90), TimeSignature.COMMON_TIME, bars));
    }

    @Test
    void followsASongsTempoMap() {
        TempoMap song = TempoMap.constant(Tempo.of(60)).withChangeAt(Fraction.whole(1), Tempo.of(120));

        SessionTiming timing = new SessionTiming(song, TimeSignature.COMMON_TIME, 1);

        assertThat(timing.tempo()).isEqualTo(Tempo.of(60));
        assertThat(timing.countInNanos()).isEqualTo(4_000_000_000L);
        assertThat(timing.tempoMap()).isEqualTo(song);
    }
}
