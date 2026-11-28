package bg.petarh.piano.midi.jdk;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Finds a General MIDI SoundFont (.sf2) for the software synthesizer: the file in {@code PP_SOUNDFONT}, then the
 * user's own {@code piano-player/soundfonts} folder, then FluidR3 or any other SoundFont in the system folders.
 */
final class SoundFonts {

    static final List<Path> SYSTEM_FOLDERS = List.of(Path.of("/usr/share/sounds/sf2"), Path.of("/usr/share/soundfonts"));

    private static final String FLUID = "FluidR3_GM.sf2";

    private SoundFonts() {
    }

    static Optional<Path> find(Function<String, String> env, Path home, List<Path> systemFolders) {
        List<Path> candidates = new ArrayList<>();
        Optional.ofNullable(env.apply("PP_SOUNDFONT")).map(Path::of).ifPresent(candidates::add);
        Path dataHome = Optional.ofNullable(env.apply("XDG_DATA_HOME")).map(Path::of)
                .orElse(home.resolve(".local/share"));
        candidates.addAll(soundFontsIn(dataHome.resolve("piano-player/soundfonts")));
        systemFolders.forEach(folder -> candidates.add(folder.resolve(FLUID)));
        systemFolders.forEach(folder -> candidates.addAll(soundFontsIn(folder)));
        return candidates.stream().filter(Files::isRegularFile).filter(Files::isReadable).findFirst();
    }

    private static List<Path> soundFontsIn(Path folder) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(file -> file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".sf2"))
                    .sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
