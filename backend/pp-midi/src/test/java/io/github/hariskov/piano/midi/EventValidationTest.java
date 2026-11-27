package io.github.hariskov.piano.midi;

import io.github.hariskov.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class EventValidationTest {

    private static final Pitch MIDDLE_C = Pitch.parse("C4");

    @ParameterizedTest
    @ValueSource(ints = {0, 128})
    void inputNoteOnRejectsVelocityOutsideAudibleRange(int velocity) {
        assertThatIllegalArgumentException().isThrownBy(() -> new InputEvent.NoteOn(MIDDLE_C, velocity, 0L));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 127})
    void inputNoteOnAcceptsAudibleVelocity(int velocity) {
        assertThatCode(() -> new InputEvent.NoteOn(MIDDLE_C, velocity, 0L)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 16})
    void outputEventsRejectChannelOutsideMidiRange(int channel) {
        assertThatIllegalArgumentException().isThrownBy(() -> new OutputEvent.NoteOn(MIDDLE_C, 64, channel));
        assertThatIllegalArgumentException().isThrownBy(() -> new OutputEvent.NoteOff(MIDDLE_C, channel));
        assertThatIllegalArgumentException().isThrownBy(() -> new OutputEvent.AllNotesOff(channel));
    }

    @Test
    void outputNoteOnRejectsSilentVelocity() {
        assertThatIllegalArgumentException().isThrownBy(() -> new OutputEvent.NoteOn(MIDDLE_C, 0, 0));
    }
}
