package bg.petarh.piano.library.settings;

import bg.petarh.piano.model.keyboard.KeyboardRange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SettingsTest {

    private final Settings defaults = Settings.defaults();

    @Test
    void startsWithNothingChosen() {
        assertThat(defaults.input()).isEmpty();
        assertThat(defaults.output()).isEmpty();
        assertThat(defaults.keyboardFor("jdk:Piano")).isEmpty();
        assertThat(defaults.tempoFor("wait")).isEmpty();
        assertThat(defaults.latencyOffsetMs()).isZero();
        assertThat(defaults.instrument()).isZero();
        assertThat(defaults.keySound()).isFalse();
    }

    @Test
    void remembersTheInstrumentAndWhetherKeysSound() {
        Settings settings = defaults.withInstrument(19).withKeySound(true);

        assertThat(settings.instrument()).isEqualTo(19);
        assertThat(settings.keySound()).isTrue();
    }

    @Test
    void rejectsAnInstrumentOutsideGeneralMidi() {
        assertThatIllegalArgumentException().isThrownBy(() -> defaults.withInstrument(128));
        assertThatIllegalArgumentException().isThrownBy(() -> defaults.withInstrument(-1));
    }

    @Test
    void remembersDevices() {
        Settings settings = defaults.withInput("jdk:Piano").withOutput("jdk:Gervill");

        assertThat(settings.input()).contains("jdk:Piano");
        assertThat(settings.output()).contains("jdk:Gervill");
    }

    @Test
    void remembersAKeyboardRangeForEachDevice() {
        KeyboardRange small = KeyboardRange.parse("C2-C7");
        Settings settings = defaults.withKeyboard("jdk:Piano", KeyboardRange.fullPiano()).withKeyboard("jdk:Small", small);

        assertThat(settings.keyboardFor("jdk:Piano")).contains(KeyboardRange.fullPiano());
        assertThat(settings.keyboardFor("jdk:Small")).contains(small);
    }

    @Test
    void remembersTheLastTempoForEachMode() {
        Settings settings = defaults.withTempo("wait", 70).withTempo("tempo", 100).withTempo("wait", 72);

        assertThat(settings.tempoFor("wait")).contains(72);
        assertThat(settings.tempoFor("tempo")).contains(100);
    }

    @Test
    void remembersTheLatencyOffset() {
        assertThat(defaults.withLatencyOffset(-25).latencyOffsetMs()).isEqualTo(-25);
    }

    @Test
    void rejectsALatencyOffsetOverHalfASecond() {
        assertThatIllegalArgumentException().isThrownBy(() -> defaults.withLatencyOffset(501));
        assertThatIllegalArgumentException().isThrownBy(() -> defaults.withLatencyOffset(-501));
    }

    @Test
    void changesNeverAffectTheOriginal() {
        defaults.withInput("jdk:Piano").withTempo("wait", 60);

        assertThat(defaults).isEqualTo(Settings.defaults());
    }
}
