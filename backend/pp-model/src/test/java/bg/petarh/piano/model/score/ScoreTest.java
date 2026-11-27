package bg.petarh.piano.model.score;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ScoreTest {

    @Test
    void usesFourFourInCAt120WhenNothingIsGiven() {
        Score score = Score.builder("Empty").length(Fraction.whole(2)).build();

        assertThat(score.timeSignatureAt(Fraction.ZERO)).isEqualTo(TimeSignature.COMMON_TIME);
        assertThat(score.keyAt(Fraction.ZERO)).isEqualTo(KeySignature.major("C"));
        assertThat(score.tempoMap().tempoAt(Fraction.ZERO)).isEqualTo(Tempo.of(120));
    }

    @Test
    void findsTheSignaturesInForceAtAPosition() {
        Score score = Score.builder("Changes")
                .timeSignature(Fraction.ZERO, TimeSignature.parse("3/4"))
                .timeSignature(Fraction.of(3, 2), TimeSignature.COMMON_TIME)
                .key(Fraction.ZERO, KeySignature.major("G"))
                .key(Fraction.whole(1), KeySignature.major("D"))
                .length(Fraction.whole(3))
                .build();

        assertThat(score.timeSignatureAt(Fraction.of(5, 4))).isEqualTo(TimeSignature.parse("3/4"));
        assertThat(score.timeSignatureAt(Fraction.of(3, 2))).isEqualTo(TimeSignature.COMMON_TIME);
        assertThat(score.keyAt(Fraction.of(3, 4))).isEqualTo(KeySignature.major("G"));
        assertThat(score.keyAt(Fraction.whole(2))).isEqualTo(KeySignature.major("D"));
    }

    @Test
    void numbersMeasuresAcrossTimeSignatureChanges() {
        Score score = Score.builder("Measures")
                .timeSignature(Fraction.ZERO, TimeSignature.parse("3/4"))
                .timeSignature(Fraction.of(3, 2), TimeSignature.COMMON_TIME)
                .length(Fraction.of(7, 2))
                .build();

        assertThat(score.measureStarts()).containsExactly(
                Fraction.ZERO, Fraction.of(3, 4), Fraction.of(3, 2), Fraction.of(5, 2));
        assertThat(score.measureIndexAt(Fraction.of(1, 2))).isZero();
        assertThat(score.measureIndexAt(Fraction.whole(2))).isEqualTo(2);
        assertThat(score.measureIndexAt(Fraction.of(5, 2))).isEqualTo(3);
    }

    @Test
    void usesTheGivenMeasureStartsForPickupsAndOddMeasures() {
        Score score = Score.builder("Pickup")
                .measureStarts(List.of(Fraction.ZERO, Fraction.of(1, 4), Fraction.of(5, 4)))
                .length(Fraction.of(9, 4))
                .build();

        assertThat(score.measureStarts()).containsExactly(Fraction.ZERO, Fraction.of(1, 4), Fraction.of(5, 4));
        assertThat(score.measureIndexAt(Fraction.of(1, 2))).isEqualTo(1);
    }

    @Test
    void rejectsMeasureStartsThatDoNotBeginAtZeroOrGoBackwards() {
        assertThatIllegalArgumentException().isThrownBy(() -> Score.builder("x")
                .measureStarts(List.of(Fraction.of(1, 4))).length(Fraction.whole(1)).build());
        assertThatIllegalArgumentException().isThrownBy(() -> Score.builder("x")
                .measureStarts(List.of(Fraction.ZERO, Fraction.of(1, 2), Fraction.of(1, 4))).length(Fraction.whole(1)).build());
    }

    @Test
    void buildsTheTempoMapFromTempoChanges() {
        Score score = Score.builder("Tempo")
                .tempo(Fraction.ZERO, Tempo.of(60))
                .tempo(Fraction.whole(1), Tempo.of(120))
                .length(Fraction.whole(2))
                .build();

        assertThat(score.tempoMap().nanosAt(Fraction.of(5, 4))).isEqualTo(4_500_000_000L);
    }

    @Test
    void keepsItsParts() {
        Part piano = new Part("P1", "Piano", 2, List.of(ScoreNoteTest.note("C4", 0, 1, 1)));

        Score score = Score.builder("Song").composer("Anon").part(piano).length(Fraction.whole(1)).build();

        assertThat(score.parts()).containsExactly(piano);
        assertThat(score.title()).isEqualTo("Song");
        assertThat(score.composer()).contains("Anon");
        assertThat(score.part("P1")).contains(piano);
    }

    @Test
    void rejectsChangesPastTheEndAndTwoPartsWithOneId() {
        assertThatIllegalArgumentException().isThrownBy(() -> Score.builder("x")
                .key(Fraction.whole(5), KeySignature.major("G")).length(Fraction.whole(1)).build());
        Part part = new Part("P1", "A", 1, List.of());
        assertThatIllegalArgumentException().isThrownBy(() -> Score.builder("x").part(part).part(part)
                .length(Fraction.whole(1)).build());
    }

    @Test
    void measuresLengthInMeasuresAndTime() {
        Score score = Score.builder("x").tempo(Fraction.ZERO, Tempo.of(60)).length(Fraction.whole(2)).build();

        assertThat(score.measureCount()).isEqualTo(2);
        assertThat(score.tempoMap().nanosAt(score.length())).isEqualTo(8_000_000_000L);
    }
}
