package bg.petarh.piano.musicxml;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.score.ScoreNote;
import bg.petarh.piano.model.score.Spelling;
import bg.petarh.piano.model.score.Tie;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class MusicXmlWriterTest {

    private static final Path SCORES = Path.of("src/test/resources/scores");

    private final MusicXmlReader reader = new MusicXmlReader();
    private final MusicXmlWriter writer = new MusicXmlWriter();

    @ParameterizedTest
    @ValueSource(strings = {"melody.musicxml", "piano.musicxml", "ties-and-triplets.musicxml", "changes.musicxml"})
    void readingWhatWasWrittenGivesTheSameScore(String file) throws IOException {
        Score original = reader.read(SCORES.resolve(file)).score();

        Score again = roundTrip(original);

        assertSameScore(again, original);
    }

    @Test
    void writesWellFormedPartwiseMusicXml() {
        String xml = write(melody());

        assertThat(xml).startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"")
                .contains("<!DOCTYPE score-partwise PUBLIC \"-//Recordare//DTD MusicXML 4.0 Partwise//EN\"")
                .contains("<score-partwise version=\"4.0\">")
                .contains("<work-title>Test &amp; Song</work-title>")
                .contains("<part-name>Melody</part-name>");
    }

    @Test
    void fillsGapsWithRestsAndEmptyMeasuresWithWholeMeasureRests() {
        String xml = write(melody());

        assertThat(xml).contains("<rest/>").contains("<rest measure=\"yes\"/>");
    }

    @Test
    void splitsANoteAcrossTheBarlineIntoTiedNotes() {
        Score score = Score.builder("Tied").length(Fraction.whole(2))
                .part(new Part("P1", "Melody", 1, List.of(note("G4", Fraction.of(3, 4), Fraction.of(1, 2), 1, 1))))
                .build();

        Score again = roundTrip(score);

        assertThat(again.parts().getFirst().notes()).extracting(ScoreNote::start, ScoreNote::duration, ScoreNote::tie)
                .containsExactly(
                        tuple(Fraction.of(3, 4), Fraction.of(1, 4), Tie.START),
                        tuple(Fraction.whole(1), Fraction.of(1, 4), Tie.STOP));
        assertThat(again.parts().getFirst().playableNotes()).hasSize(1);
        assertThat(write(score)).contains("<tied type=\"start\"/>");
    }

    @Test
    void writesNoteTypesDotsAndTriplets() {
        Score score = Score.builder("Values").length(Fraction.whole(1))
                .part(new Part("P1", "Melody", 1, List.of(
                        note("C4", Fraction.ZERO, Fraction.of(3, 8), 1, 1),
                        note("D4", Fraction.of(3, 8), Fraction.of(1, 8), 1, 1),
                        note("E4", Fraction.of(1, 2), Fraction.of(1, 12), 1, 1),
                        note("F4", Fraction.of(7, 12), Fraction.of(1, 12), 1, 1),
                        note("G4", Fraction.of(2, 3), Fraction.of(1, 12), 1, 1),
                        note("A4", Fraction.of(3, 4), Fraction.of(1, 4), 1, 1))))
                .build();

        String xml = write(score);

        assertThat(xml).contains("<type>quarter</type>", "<dot/>", "<type>eighth</type>", "<actual-notes>3</actual-notes>",
                "<normal-notes>2</normal-notes>", "<divisions>6</divisions>");
        assertSameScore(roundTrip(score), score);
    }

    @Test
    void writesTwoStavesWithClefsAndChords() {
        Score score = Score.builder("Piano").length(Fraction.whole(1))
                .key(Fraction.ZERO, KeySignature.major("F"))
                .timeSignature(Fraction.ZERO, TimeSignature.COMMON_TIME)
                .tempo(Fraction.ZERO, Tempo.of(72))
                .part(new Part("P1", "Piano", 2, List.of(
                        note("F4", Fraction.ZERO, Fraction.whole(1), 1, 1),
                        note("A4", Fraction.ZERO, Fraction.whole(1), 1, 1),
                        note("F2", Fraction.ZERO, Fraction.of(1, 2), 2, 2),
                        note("C3", Fraction.of(1, 2), Fraction.of(1, 2), 2, 2))))
                .build();

        String xml = write(score);

        assertThat(xml).contains("<staves>2</staves>", "<clef number=\"1\">", "<sign>G</sign>", "<clef number=\"2\">",
                "<sign>F</sign>", "<chord/>", "<backup>", "<sound tempo=\"72\"/>", "<fifths>-1</fifths>");
        assertSameScore(roundTrip(score), score);
    }

    @Test
    void givesAStaffWithNothingToPlayInAMeasureAWholeMeasureRest() {
        Score score = Score.builder("Piano").length(Fraction.whole(1))
                .part(new Part("P1", "Piano", 2, List.of(note("E5", Fraction.ZERO, Fraction.whole(1), 1, 1))))
                .build();

        String xml = write(score);

        int rest = xml.indexOf("<rest measure=\"yes\"/>");
        assertThat(rest).isPositive();
        assertThat(xml.substring(rest, xml.indexOf("</note>", rest))).contains("<staff>2</staff>");
        assertSameScore(roundTrip(score), score);
    }

    private static Score melody() {
        return Score.builder("Test & Song").length(Fraction.whole(2))
                .part(new Part("P1", "Melody", 1, List.of(
                        note("C4", Fraction.ZERO, Fraction.of(1, 4), 1, 1),
                        note("E4", Fraction.of(1, 2), Fraction.of(1, 2), 1, 1))))
                .build();
    }

    private static ScoreNote note(String pitch, Fraction start, Fraction duration, int staff, int voice) {
        return new ScoreNote(Spelling.of(Pitch.parse(pitch), false), start, duration, staff, voice, Tie.NONE);
    }

    private String write(Score score) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(score, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private Score roundTrip(Score score) {
        byte[] xml = write(score).getBytes(StandardCharsets.UTF_8);
        return reader.read(new ByteArrayInputStream(xml), "round-trip.musicxml").score();
    }

    private static void assertSameScore(Score actual, Score expected) {
        assertThat(actual.title()).isEqualTo(expected.title());
        assertThat(actual.composer()).isEqualTo(expected.composer());
        assertThat(actual.length()).isEqualTo(expected.length());
        assertThat(actual.measureStarts()).isEqualTo(expected.measureStarts());
        assertThat(actual.timeSignatures()).isEqualTo(expected.timeSignatures());
        assertThat(actual.keys()).isEqualTo(expected.keys());
        assertThat(actual.tempos()).isEqualTo(expected.tempos());
        assertThat(actual.parts()).hasSameSizeAs(expected.parts());
        for (int i = 0; i < expected.parts().size(); i++) {
            Part want = expected.parts().get(i);
            Part got = actual.parts().get(i);
            assertThat(got.id()).isEqualTo(want.id());
            assertThat(got.name()).isEqualTo(want.name());
            assertThat(got.staffCount()).isEqualTo(want.staffCount());
            assertThat(got.notes()).containsExactlyInAnyOrderElementsOf(want.notes());
        }
    }
}
