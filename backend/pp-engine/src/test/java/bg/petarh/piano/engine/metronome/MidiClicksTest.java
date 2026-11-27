package bg.petarh.piano.engine.metronome;

import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.midi.output.OutputEvent;
import bg.petarh.piano.midi.value.MidiChannel;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MidiClicksTest {

    private static final MidiChannel PERCUSSION = MidiChannel.of(9);

    private final SilentOutput silent = SilentOutput.create();
    private final OutputConnection output = silent.port().open(DeviceId.of("virtual:silent"));
    private final MidiClicks clicks = new MidiClicks(output);

    @Test
    void playsAnAccentedClickOnTheHighWoodBlock() {
        clicks.click(true);

        assertThat(silent.sentEvents()).containsExactly(
                new OutputEvent.NoteOn(Pitch.of(76), Velocity.of(110), PERCUSSION),
                new OutputEvent.NoteOff(Pitch.of(76), PERCUSSION));
    }

    @Test
    void playsOtherBeatsOnTheLowWoodBlock() {
        clicks.click(false);

        assertThat(silent.sentEvents()).containsExactly(
                new OutputEvent.NoteOn(Pitch.of(77), Velocity.of(80), PERCUSSION),
                new OutputEvent.NoteOff(Pitch.of(77), PERCUSSION));
    }
}
