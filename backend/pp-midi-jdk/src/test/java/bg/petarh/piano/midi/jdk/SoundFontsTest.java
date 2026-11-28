package bg.petarh.piano.midi.jdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SoundFontsTest {

    @TempDir
    Path root;

    @Test
    void usesTheFileNamedByPpSoundfont() throws IOException {
        Path chosen = soundFont(root.resolve("mine/Grand.sf2"));
        soundFont(system().resolve("FluidR3_GM.sf2"));

        assertThat(find(Map.of("PP_SOUNDFONT", chosen.toString()))).contains(chosen);
    }

    @Test
    void prefersYourOwnFolderToTheSystemOnes() throws IOException {
        Path own = soundFont(root.resolve("home/.local/share/piano-player/soundfonts/Steinway.sf2"));
        soundFont(system().resolve("FluidR3_GM.sf2"));

        assertThat(find(Map.of())).contains(own);
    }

    @Test
    void followsXdgDataHomeForYourOwnFolder() throws IOException {
        Path own = soundFont(root.resolve("data/piano-player/soundfonts/Organ.sf2"));

        assertThat(find(Map.of("XDG_DATA_HOME", root.resolve("data").toString()))).contains(own);
    }

    @Test
    void prefersFluidR3AmongTheSystemSoundFonts() throws IOException {
        soundFont(system().resolve("Another.sf2"));
        Path fluid = soundFont(system().resolve("FluidR3_GM.sf2"));

        assertThat(find(Map.of())).contains(fluid);
    }

    @Test
    void takesAnyOtherSystemSoundFontWithoutFluidR3() throws IOException {
        Path other = soundFont(system().resolve("TimGM6mb.sf2"));
        Files.writeString(system().resolve("readme.txt"), "not a sound font");

        assertThat(find(Map.of())).contains(other);
    }

    @Test
    void findsNothingWhenNoneIsInstalledOrTheNamedFileIsMissing() {
        assertThat(find(Map.of("PP_SOUNDFONT", root.resolve("missing.sf2").toString()))).isEmpty();
    }

    private java.util.Optional<Path> find(Map<String, String> env) {
        return SoundFonts.find(env::get, root.resolve("home"), List.of(system(), root.resolve("usr/share/soundfonts")));
    }

    private Path system() {
        return root.resolve("usr/share/sounds/sf2");
    }

    private static Path soundFont(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        return Files.write(file, new byte[] {'R', 'I', 'F', 'F'});
    }
}
