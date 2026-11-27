package bg.petarh.piano.musicxml;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Placed;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.score.ScoreNote;
import bg.petarh.piano.model.score.Tie;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class MusicXmlReaderTest {

    private static final Path SCORES = Path.of("src/test/resources/scores");

    private final MusicXmlReader reader = new MusicXmlReader();

    @TempDir
    Path folder;

    @Test
    void readsASimpleMelody() throws IOException {
        ImportResult result = reader.read(SCORES.resolve("melody.musicxml"));
        Score score = result.score();
        Part flute = score.parts().getFirst();

        assertThat(score.title()).isEqualTo("Simple Melody");
        assertThat(score.composer()).contains("Anon");
        assertThat(flute.id()).isEqualTo("P1");
        assertThat(flute.name()).isEqualTo("Flute");
        assertThat(flute.staffCount()).isEqualTo(1);
        assertThat(flute.notes()).extracting(note -> note.spelling() + "@" + note.start() + "/" + note.duration())
                .containsExactly("C4@0/1/4", "D4@1/4/1/4", "F#4@3/4/1/8", "G4@7/8/1/8", "Bb4@1/1");
        assertThat(score.tempoMap().tempoAt(Fraction.ZERO)).isEqualTo(Tempo.of(100));
        assertThat(score.timeSignatureAt(Fraction.ZERO)).isEqualTo(TimeSignature.COMMON_TIME);
        assertThat(score.length()).isEqualTo(Fraction.whole(2));
        assertThat(result.warnings()).isEmpty();
    }

    @Test
    void readsTwoStavesChordsAndVoices() throws IOException {
        Score score = reader.read(SCORES.resolve("piano.musicxml")).score();
        Part piano = score.parts().getFirst();

        assertThat(score.title()).isEqualTo("Two Hands");
        assertThat(piano.staffCount()).isEqualTo(2);
        assertThat(score.keyAt(Fraction.ZERO)).isEqualTo(KeySignature.major("G"));
        assertThat(score.timeSignatureAt(Fraction.ZERO)).isEqualTo(TimeSignature.parse("3/4"));
        assertThat(piano.notes()).extracting(note -> note.spelling() + "@" + note.start() + " staff " + note.staff()
                        + " voice " + note.voice())
                .containsExactly("B4@0 staff 1 voice 1", "D5@0 staff 1 voice 1", "A4@1/4 staff 1 voice 1",
                        "G2@0 staff 2 voice 5");
        assertThat(piano.playableNotes()).filteredOn(note -> note.hand() == Hand.LEFT)
                .extracting(NoteEvent::duration).containsExactly(Fraction.of(3, 4));
    }

    @Test
    void readsTupletsAsExactDurationsAndTiesAcrossBarlines() throws IOException {
        Part violin = reader.read(SCORES.resolve("ties-and-triplets.musicxml")).score().parts().getFirst();

        assertThat(violin.notes()).extracting(ScoreNote::start, ScoreNote::duration, ScoreNote::tie).containsExactly(
                tuple(Fraction.ZERO, Fraction.of(1, 12), Tie.NONE),
                tuple(Fraction.of(1, 12), Fraction.of(1, 12), Tie.NONE),
                tuple(Fraction.of(1, 6), Fraction.of(1, 12), Tie.NONE),
                tuple(Fraction.of(1, 4), Fraction.of(1, 4), Tie.START),
                tuple(Fraction.of(1, 2), Fraction.of(1, 2), Tie.STOP));
        assertThat(violin.playableNotes().getLast().duration()).isEqualTo(Fraction.of(3, 4));
    }

    @Test
    void readsPickupMeasuresAndChangesOfKeyTimeAndTempo() throws IOException {
        Score score = reader.read(SCORES.resolve("changes.musicxml")).score();

        assertThat(score.measureStarts()).containsExactly(Fraction.ZERO, Fraction.of(1, 4), Fraction.of(5, 4));
        assertThat(score.keys()).containsExactly(
                new Placed<>(Fraction.ZERO, KeySignature.minor("D")), new Placed<>(Fraction.of(5, 4), KeySignature.major("D")));
        assertThat(score.timeSignatureAt(Fraction.of(5, 4))).isEqualTo(TimeSignature.parse("6/8"));
        assertThat(score.tempos()).containsExactly(
                new Placed<>(Fraction.ZERO, Tempo.of(80)), new Placed<>(Fraction.of(5, 4), Tempo.of(132)));
        assertThat(score.length()).isEqualTo(Fraction.whole(2));
    }

    @Test
    void skipsWhatItCannotPlayAndReportsIt() throws IOException {
        ImportResult result = reader.read(SCORES.resolve("unsupported.musicxml"));
        Part piano = result.score().part("P1").orElseThrow();

        assertThat(piano.notes()).extracting(note -> note.spelling().toString()).containsExactly("C4", "E4");
        assertThat(result.score().part("P2").orElseThrow().notes()).isEmpty();
        assertThat(result.warnings()).anySatisfy(warning -> assertThat(warning).containsIgnoringCase("grace notes"))
                .anySatisfy(warning -> assertThat(warning).containsIgnoringCase("repeats"))
                .anySatisfy(warning -> assertThat(warning).contains("Drums").contains("unpitched"))
                .anySatisfy(warning -> assertThat(warning).containsIgnoringCase("microtone"));
    }

    @Test
    void reportsEachKindOfProblemOnce() throws IOException {
        ImportResult result = reader.read(SCORES.resolve("unsupported.musicxml"));

        assertThat(result.warnings()).doesNotHaveDuplicates();
        assertThat(result.warnings().stream().filter(warning -> warning.contains("Repeats"))).hasSize(1);
    }

    @Test
    void refusesFilesThatAreNotWellFormed() {
        assertThatThrownBy(() -> reader.read(SCORES.resolve("broken.musicxml")))
                .isInstanceOf(MusicXmlException.class).hasMessageContaining("broken.musicxml");
    }

    @Test
    void refusesTimewiseScores() {
        assertThatThrownBy(() -> reader.read(SCORES.resolve("timewise.musicxml")))
                .isInstanceOf(MusicXmlException.class).hasMessageContaining("score-timewise");
    }

    @Test
    void neverReadsExternalEntities() throws IOException {
        Path secret = folder.resolve("secret.txt");
        Files.writeString(secret, "TOP-SECRET");
        Path score = folder.resolve("entity.musicxml");
        Files.writeString(score, Files.readString(SCORES.resolve("external-entity.musicxml"))
                .replace("file:///etc/hostname", secret.toUri().toString()));

        try {
            assertThat(reader.read(score).score().title()).doesNotContain("TOP-SECRET");
        } catch (MusicXmlException refused) {
            assertThat(refused).hasMessageNotContaining("TOP-SECRET");
        }
    }

    @Test
    void readsCompressedMxlFilesThroughTheirContainer() throws IOException {
        Path mxl = folder.resolve("song.mxl");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(mxl))) {
            add(zip, "META-INF/container.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <container><rootfiles><rootfile full-path="music/song.musicxml"/></rootfiles></container>""");
            add(zip, "music/song.musicxml", Files.readString(SCORES.resolve("melody.musicxml")));
        }

        assertThat(reader.read(mxl).score().title()).isEqualTo("Simple Melody");
    }

    @Test
    void readsMxlFilesWithoutAContainerFromTheirScoreFile() throws IOException {
        Path mxl = folder.resolve("bare.mxl");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(mxl))) {
            add(zip, "piano.xml", Files.readString(SCORES.resolve("piano.musicxml")));
        }

        assertThat(reader.read(mxl).score().title()).isEqualTo("Two Hands");
    }

    @Test
    void namesUntitledScoresAfterTheirFile() throws IOException {
        Path untitled = folder.resolve("Night Song.musicxml");
        Files.writeString(untitled, Files.readString(SCORES.resolve("ties-and-triplets.musicxml")));

        assertThat(reader.read(untitled).score().title()).isEqualTo("Night Song");
    }

    private static void add(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        OutputStream entry = zip;
        entry.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
