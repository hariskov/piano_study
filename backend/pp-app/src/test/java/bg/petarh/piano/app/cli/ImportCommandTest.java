package bg.petarh.piano.app.cli;

import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ImportCommandTest {

    private static final String MELODY = """
            <?xml version="1.0" encoding="UTF-8"?>
            <score-partwise version="4.0">
              <work><work-title>Simple Melody</work-title></work>
              <part-list><score-part id="P1"><part-name>Flute</part-name></score-part></part-list>
              <part id="P1"><measure number="1"><attributes><divisions>1</divisions></attributes>
                <note><pitch><step>C</step><octave>5</octave></pitch><duration>2</duration><voice>1</voice></note>
                <note><pitch><step>G</step><octave>5</octave></pitch><duration>2</duration><voice>1</voice></note>
              </measure></part>
            </score-partwise>
            """;

    @TempDir
    Path folder;

    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();

    @Test
    void importsAMidiFileIntoTheLibrary() throws IOException {
        Path midi = Files.write(folder.resolve("Little Tune.mid"), midiFile(60, 72));

        int exitCode = run("import", midi.toString(), "--library", library().toString());

        assertThat(exitCode).isZero();
        assertThat(out.toString())
                .contains("Imported \"Little Tune\" as little-tune (MIDI, 1 part, 1 measure)")
                .contains("P1  Acoustic Grand Piano  2 staves  2 notes  C4-C5")
                .contains("estimated as C major");
        assertThat(library().resolve("little-tune/score.musicxml")).exists();
    }

    @Test
    void writesAMidiFileAsMusicXmlWhenAsked() throws IOException {
        Path midi = Files.write(folder.resolve("tune.mid"), midiFile(64, 67));
        Path musicXml = folder.resolve("tune.musicxml");

        int exitCode = run("import", midi.toString(), "--out", musicXml.toString(), "--split", "G4");

        assertThat(exitCode).isZero();
        assertThat(musicXml).content().contains("<score-partwise", "<staves>2</staves>");
        assertThat(out.toString()).contains("Wrote " + musicXml);
    }

    @Test
    void addsMusicXmlFilesToTheLibraryAndListsThem() throws IOException {
        Path file = Files.writeString(folder.resolve("melody.musicxml"), MELODY);
        run("import", file.toString(), "--library", library().toString());

        int exitCode = run("songs", "--library", library().toString());

        assertThat(exitCode).isZero();
        assertThat(out.toString()).contains("Imported \"Simple Melody\" as simple-melody (MusicXML, 1 part, 1 measure)")
                .contains("simple-melody  Simple Melody  1 part  1 measure  MusicXML");
    }

    @Test
    void saysWhenTheLibraryIsEmpty() {
        run("songs", "--library", library().toString());

        assertThat(out.toString()).contains("No songs yet");
    }

    @Test
    void reportsFilesItCannotReadWithoutAStackTrace() throws IOException {
        Path broken = Files.writeString(folder.resolve("broken.mid"), "not midi");

        int exitCode = run("import", broken.toString(), "--library", library().toString());

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("broken.mid").doesNotContain("\tat ");
    }

    private Path library() {
        return folder.resolve("library");
    }

    private int run(String... args) {
        CliEnvironment environment = new CliEnvironment(VirtualKeyboard.withClock(() -> 0L).port(),
                SilentOutput.create().port(), () -> 0L, new ScriptedLifetime());
        return PianoCli.create(environment, new PrintWriter(out, true), new PrintWriter(err, true)).execute(args);
    }

    private static byte[] midiFile(int lowPitch, int highPitch) {
        ByteArrayOutputStream track = new ByteArrayOutputStream();
        int[] events = {
                0x00, 0x90, lowPitch, 0x50,
                0x00, 0x90, highPitch, 0x50,
                0x87, 0x40, 0x80, lowPitch, 0x00,
                0x00, 0x80, highPitch, 0x00,
                0x00, 0xFF, 0x2F, 0x00};
        for (int value : events) {
            track.write(value);
        }
        ByteArrayOutputStream file = new ByteArrayOutputStream();
        file.writeBytes(new byte[]{'M', 'T', 'h', 'd', 0, 0, 0, 6, 0, 0, 0, 1, 0x01, (byte) 0xE0});
        file.writeBytes(new byte[]{'M', 'T', 'r', 'k', 0, 0, 0, (byte) track.size()});
        file.writeBytes(track.toByteArray());
        return file.toByteArray();
    }
}
