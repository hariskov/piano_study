package bg.petarh.piano.api.sound;

import bg.petarh.piano.library.settings.Settings;
import bg.petarh.piano.midi.output.InputEcho;
import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.midi.value.Instrument;
import bg.petarh.piano.midi.value.MidiChannel;

import java.util.Optional;
import java.util.function.Function;

/** Plays the keys you press through the saved output, when the settings ask for it (DEV-14). */
public final class KeySound {

    // Playback uses channel 2 and the metronome channel 10, so your own keys get the first channel.
    static final MidiChannel CHANNEL = MidiChannel.of(0);

    private KeySound() {
    }

    public static Optional<String> output(Settings settings) {
        return settings.keySound() ? settings.output() : Optional.empty();
    }

    public static Optional<InputEcho> echo(Settings settings, Function<String, OutputConnection> outputs) {
        return output(settings).map(outputs)
                .map(output -> InputEcho.playing(Instrument.of(settings.instrument()), output, CHANNEL));
    }
}
