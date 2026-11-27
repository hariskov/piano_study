package bg.petarh.piano.midi.value;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MidiChannelTest {

    @ParameterizedTest
    @ValueSource(ints = {-1, 16})
    void rejectsIndexOutsideMidiChannels(int index) {
        assertThatIllegalArgumentException().isThrownBy(() -> MidiChannel.of(index));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9, 15})
    void keepsValidIndex(int index) {
        assertThat(MidiChannel.of(index).index()).isEqualTo(index);
    }

    @Test
    void channelsWithSameIndexAreEqual() {
        assertThat(MidiChannel.of(3)).isEqualTo(MidiChannel.of(3)).hasSameHashCodeAs(MidiChannel.of(3));
        assertThat(MidiChannel.of(3)).isNotEqualTo(MidiChannel.of(4));
    }
}
