package bg.petarh.piano.library.songs;

import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.score.ScoreNote;
import bg.petarh.piano.model.score.Spelling;
import bg.petarh.piano.model.score.Tie;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.musicxml.MusicXmlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SongLibraryTest {

    private static final Instant NOW = Instant.parse("2026-11-27T20:00:00Z");
    private static final String MELODY = """
            <?xml version="1.0" encoding="UTF-8"?>
            <score-partwise version="4.0">
              <work><work-title>Simple Melody</work-title></work>
              <part-list><score-part id="P1"><part-name>Flute</part-name></score-part></part-list>
              <part id="P1">
                <measure number="1"><attributes><divisions>1</divisions></attributes>
                  <note><pitch><step>C</step><octave>4</octave></pitch><duration>4</duration><voice>1</voice></note>
                </measure>
                <measure number="2">
                  <barline location="left"><repeat direction="forward"/></barline>
                  <note><pitch><step>D</step><octave>4</octave></pitch><duration>4</duration><voice>1</voice></note>
                </measure>
              </part>
            </score-partwise>
            """;

    @TempDir
    Path folder;

    private SongLibrary library() {
        return new SongLibrary(folder.resolve("library"), () -> NOW);
    }

    @Test
    void importsAMusicXmlFileAsItIs() throws IOException {
        Path file = write("Melody File.musicxml", MELODY);

        SongEntry entry = library().importMusicXml(file);

        assertThat(entry.id()).isEqualTo("simple-melody");
        assertThat(entry.title()).isEqualTo("Simple Melody");
        assertThat(entry.partCount()).isEqualTo(1);
        assertThat(entry.measureCount()).isEqualTo(2);
        assertThat(entry.sourceFile()).isEqualTo("Melody File.musicxml");
        assertThat(entry.format()).isEqualTo("MusicXML");
        assertThat(entry.importedAt()).isEqualTo(NOW);
        assertThat(entry.warnings()).containsExactly("Repeats are played straight through");
        assertThat(entry.fileName()).isEqualTo("Simple Melody.musicxml");
        assertThat(folder.resolve("library/xml/Simple Melody.musicxml")).hasContent(MELODY);
        assertThat(folder.resolve("library/index.json")).content().contains("\"title\" : \"Simple Melody\"");
    }

    @Test
    void storesTheScoreOfACompressedFile() throws IOException {
        Path mxl = folder.resolve("song.mxl");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(mxl))) {
            zip.putNextEntry(new ZipEntry("song.xml"));
            OutputStream entry = zip;
            entry.write(MELODY.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }

        SongEntry entry = library().importMusicXml(mxl);

        assertThat(entry.format()).isEqualTo("MusicXML (compressed)");
        assertThat(library().load(entry.id()).orElseThrow().score().title()).isEqualTo("Simple Melody");
    }

    @Test
    void addsAScoreMadeElsewhere() {
        Score score = Score.builder("From MIDI").length(Fraction.whole(1))
                .part(new Part("P1", "Piano", 1, List.of(new ScoreNote(Spelling.of('E', 0, 4), Fraction.ZERO,
                        Fraction.whole(1), 1, 1, Tie.NONE))))
                .build();

        SongEntry entry = library().add(score, "song.mid", "MIDI", List.of("Quantised to 1/16"));

        Score loaded = library().load(entry.id()).orElseThrow().score();
        assertThat(loaded.parts().getFirst().notes()).isEqualTo(score.parts().getFirst().notes());
        assertThat(entry.format()).isEqualTo("MIDI");
        assertThat(entry.warnings()).containsExactly("Quantised to 1/16");
    }

    @Test
    void keepsTheOriginalMidiFileNextToItsMusicXml() throws IOException {
        Path midi = Files.write(folder.resolve("tune.mid"), new byte[]{'M', 'T', 'h', 'd'});
        Score score = Score.builder("Little Tune").length(Fraction.whole(1)).build();

        SongEntry entry = library().addMidi(score, midi, List.of());

        assertThat(entry.midiFile()).contains("Little Tune.mid");
        assertThat(folder.resolve("library/midi/Little Tune.mid")).hasBinaryContent(new byte[]{'M', 'T', 'h', 'd'});
        assertThat(folder.resolve("library/xml/Little Tune.musicxml")).exists();
        assertThat(entry.format()).isEqualTo("MIDI");
        assertThat(library().delete(entry.id())).isTrue();
        assertThat(folder.resolve("library/midi/Little Tune.mid")).doesNotExist();
    }

    @Test
    void preparesAFolderForSheets() throws IOException {
        library().importMusicXml(write("one.musicxml", MELODY));

        assertThat(folder.resolve("library/sheets")).isDirectory();
        assertThat(folder.resolve("library/midi")).isDirectory();
    }

    @Test
    void listsSongsByTitleAcrossRestarts() throws IOException {
        library().importMusicXml(write("b.musicxml", MELODY.replace("Simple Melody", "Zebra")));
        library().importMusicXml(write("a.musicxml", MELODY.replace("Simple Melody", "Aria")));

        assertThat(library().list()).extracting(SongEntry::title).containsExactly("Aria", "Zebra");
    }

    @Test
    void givesSongsWithTheSameTitleTheirOwnIds() throws IOException {
        SongEntry first = library().importMusicXml(write("one.musicxml", MELODY));
        SongEntry second = library().importMusicXml(write("two.musicxml", MELODY));

        assertThat(first.id()).isEqualTo("simple-melody");
        assertThat(second.id()).isEqualTo("simple-melody-2");
        assertThat(second.fileName()).isEqualTo("Simple Melody (2).musicxml");
        assertThat(folder.resolve("library/xml/Simple Melody (2).musicxml")).exists();
    }

    @Test
    void makesIdsFromAnyTitle() throws IOException {
        SongEntry entry = library().importMusicXml(write("x.musicxml", MELODY.replace("Simple Melody", "Für Elise / WoO 59!")));

        assertThat(entry.id()).isEqualTo("fur-elise-woo-59");
    }

    @Test
    void deletesSongs() throws IOException {
        SongEntry entry = library().importMusicXml(write("one.musicxml", MELODY));

        assertThat(library().delete(entry.id())).isTrue();

        assertThat(library().list()).isEmpty();
        assertThat(folder.resolve("library/xml/Simple Melody.musicxml")).doesNotExist();
        assertThat(library().delete("simple-melody")).isFalse();
        assertThat(library().load("simple-melody")).isEmpty();
    }

    @Test
    void refusesIdsThatLeaveTheLibrary() {
        assertThat(library().load("../outside")).isEmpty();
        assertThat(library().delete("../outside")).isFalse();
    }

    @Test
    void storesNothingForAFileItCannotRead() throws IOException {
        Path broken = write("broken.musicxml", "<score-partwise><part-list>");

        assertThatThrownBy(() -> library().importMusicXml(broken)).isInstanceOf(MusicXmlException.class);
        assertThat(library().list()).isEmpty();
    }

    @Test
    void opensSongsByNameOrId() throws IOException {
        library().importMusicXml(write("one.musicxml", MELODY));

        assertThat(library().find("simple melody")).get().extracting(SongEntry::id).isEqualTo("simple-melody");
        assertThat(library().find("SIMPLE-MELODY")).isPresent();
        assertThat(library().find("Simple Melody.musicxml")).isPresent();
        assertThat(library().find("unknown")).isEmpty();
    }

    @Test
    void namesFilesSafelyWhateverTheTitle() throws IOException {
        SongEntry entry = library().importMusicXml(write("x.musicxml", MELODY.replace("Simple Melody", "AC/DC: Live?")));

        assertThat(entry.fileName()).isEqualTo("AC-DC- Live-.musicxml");
        assertThat(folder.resolve("library/xml").resolve(entry.fileName())).exists();
    }

    @Test
    void forgetsSongsWhoseFileWasRemoved() throws IOException {
        SongEntry entry = library().importMusicXml(write("one.musicxml", MELODY));
        Files.delete(folder.resolve("library/xml").resolve(entry.fileName()));

        assertThat(library().list()).isEmpty();
        assertThat(library().load(entry.id())).isEmpty();
    }

    @Test
    void movesACorruptIndexAsideAndStartsAgain() throws IOException {
        Files.createDirectories(folder.resolve("library"));
        Files.writeString(folder.resolve("library/index.json"), "{ not json");

        assertThat(library().list()).isEmpty();
        assertThat(folder.resolve("library/index.json.bad")).hasContent("{ not json");
        assertThat(library().importMusicXml(write("one.musicxml", MELODY)).id()).isEqualTo("simple-melody");
    }

    @Test
    void usesTheXdgDataFolder() {
        assertThat(SongLibrary.defaultFolder(Map.of("XDG_DATA_HOME", "/data"), Path.of("/home/ana")))
                .isEqualTo(Path.of("/data/piano-player/musicFiles"));
        assertThat(SongLibrary.defaultFolder(Map.of(), Path.of("/home/ana")))
                .isEqualTo(Path.of("/home/ana/.local/share/piano-player/musicFiles"));
    }

    private Path write(String name, String content) throws IOException {
        Path file = folder.resolve(name);
        Files.writeString(file, content);
        return file;
    }
}
