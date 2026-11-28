package bg.petarh.piano.importmidi;

import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class RealMidiFilesTest {

    @Test
    void readsBachsMinuetAsOnePianoPartWithItsTitle() throws IOException, URISyntaxException {
        Score score = convert("bach-minuet-in-g.mid", "bach-minuet-in-g").score();

        assertThat(score.title()).isEqualTo("Menuet in G");
        assertThat(score.parts()).singleElement().satisfies(part -> {
            assertThat(part.staffCount()).isEqualTo(2);
            assertThat(part.notes()).hasSize(204);
        });
        assertThat(score.measureCount()).isEqualTo(32);
    }

    @Test
    void joinsSchubertsVoiceTracksIntoOnePianoPartAndIgnoresTheControlTrackName() throws IOException, URISyntaxException {
        MidiConversion converted = convert("schubert-impromptu-d899-3.mid", "Impromptu in G-flat");
        Score score = converted.score();

        assertThat(score.title()).isEqualTo("Impromptu in G-flat");
        assertThat(score.parts()).singleElement().satisfies(part -> {
            assertThat(part.name()).isEqualTo("Piano");
            assertThat(part.notes()).hasSize(336 + 1557 + 477 + 41 + 319);
            assertThat(part.notes()).filteredOn(note -> note.staff() == 2).hasSize(477 + 41 + 319);
        });
        assertThat(score.parts()).extracting(Part::id).containsExactly("P1");
    }

    private static MidiConversion convert(String resource, String fileTitle) throws IOException, URISyntaxException {
        Path file = Path.of(Objects.requireNonNull(RealMidiFilesTest.class.getResource("/midi/" + resource)).toURI());
        return new MidiToScore().convert(new MidiFileReader().read(file), MidiImportOptions.defaults(), fileTitle);
    }
}
