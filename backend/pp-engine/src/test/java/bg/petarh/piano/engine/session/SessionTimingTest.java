package bg.petarh.piano.engine.session;

import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
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
}
