package bg.petarh.piano.midi.value;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstrumentTest {

    @Test
    void isAGeneralMidiProgramNumber() {
        assertThat(Instrument.of(56).program()).isEqualTo(56);
        assertThat(Instrument.GRAND_PIANO).isEqualTo(Instrument.of(0));
    }

    @Test
    void refusesNumbersOutsideTheGeneralMidiSet() {
        assertThatThrownBy(() -> Instrument.of(128)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Instrument out of range 0-127: 128");
        assertThatThrownBy(() -> Instrument.of(-1)).isInstanceOf(IllegalArgumentException.class);
    }
}
