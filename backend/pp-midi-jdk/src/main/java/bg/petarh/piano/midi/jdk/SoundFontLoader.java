package bg.petarh.piano.midi.jdk;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

/** Reads the SoundFont once, on first use, since a full General MIDI set is over 100 MB. */
final class SoundFontLoader implements Supplier<Optional<Soundbank>> {

    private final Supplier<Optional<Path>> file;
    private Optional<Soundbank> loaded;

    SoundFontLoader(Supplier<Optional<Path>> file) {
        this.file = file;
    }

    @Override
    public synchronized Optional<Soundbank> get() {
        if (loaded == null) {
            loaded = file.get().flatMap(SoundFontLoader::read);
        }
        return loaded;
    }

    private static Optional<Soundbank> read(Path path) {
        try {
            return Optional.of(MidiSystem.getSoundbank(path.toFile()));
        } catch (InvalidMidiDataException | IOException e) {
            System.err.println("The SoundFont " + path + " can't be read, so the built-in sounds are used: " + e.getMessage());
            return Optional.empty();
        }
    }
}
