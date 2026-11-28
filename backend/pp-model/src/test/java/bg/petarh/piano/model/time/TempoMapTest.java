package bg.petarh.piano.model.time;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TempoMapTest {

    private static final long MS = 1_000_000L;
    private static final Fraction QUARTER = Fraction.of(1, 4);

    private final TempoMap steady = TempoMap.constant(Tempo.of(120));

    @Test
    void convertsPositionsToTimeAtASteadyTempo() {
        assertThat(steady.nanosAt(QUARTER)).isEqualTo(500 * MS);
        assertThat(steady.nanosAt(Fraction.whole(1))).isEqualTo(2000 * MS);
    }

    @Test
    void convertsTimeBackToPositions() {
        assertThat(steady.positionAt(500 * MS)).isEqualTo(QUARTER);
        assertThat(steady.positionAt(750 * MS)).isEqualTo(Fraction.of(3, 8));
    }

    @Test
    void positionsBeforeTheStartUseTheFirstTempo() {
        assertThat(steady.nanosAt(Fraction.of(-1, 4))).isEqualTo(-500 * MS);
        assertThat(steady.positionAt(-500 * MS)).isEqualTo(Fraction.of(-1, 4));
    }

    @Test
    void followsATempoChange() {
        TempoMap changed = steady.withChangeAt(Fraction.whole(1), Tempo.of(60));

        assertThat(changed.nanosAt(Fraction.whole(1))).isEqualTo(2000 * MS);
        assertThat(changed.nanosAt(Fraction.of(5, 4))).isEqualTo(3000 * MS);
        assertThat(changed.positionAt(3000 * MS)).isEqualTo(Fraction.of(5, 4));
        assertThat(changed.nanosAt(Fraction.of(1, 2))).isEqualTo(1000 * MS);
    }

    @Test
    void laterChangeAtTheSamePlaceReplacesTheEarlierOne() {
        TempoMap changed = steady.withChangeAt(Fraction.whole(1), Tempo.of(60))
                .withChangeAt(Fraction.whole(1), Tempo.of(240));

        assertThat(changed.nanosAt(Fraction.of(5, 4))).isEqualTo(2250 * MS);
        assertThat(changed.tempoAt(Fraction.whole(2))).isEqualTo(Tempo.of(240));
    }

    @Test
    void changeBeforeLaterChangesDropsThem() {
        TempoMap changed = steady.withChangeAt(Fraction.whole(2), Tempo.of(60))
                .withChangeAt(Fraction.whole(1), Tempo.of(240));

        assertThat(changed.tempoAt(Fraction.whole(3))).isEqualTo(Tempo.of(240));
    }

    @Test
    void knowsTheTempoAtEachPosition() {
        TempoMap changed = steady.withChangeAt(Fraction.whole(1), Tempo.of(60));

        assertThat(changed.tempoAt(Fraction.of(3, 4))).isEqualTo(Tempo.of(120));
        assertThat(changed.tempoAt(Fraction.whole(1))).isEqualTo(Tempo.of(60));
        assertThat(changed.latestTempo()).isEqualTo(Tempo.of(60));
    }

    @Test
    void rejectsChangesBeforeTheStart() {
        assertThatIllegalArgumentException().isThrownBy(() -> steady.withChangeAt(Fraction.of(-1, 4), Tempo.of(60)));
    }

    @Test
    void scalesEveryTempoByAFactor() {
        TempoMap song = steady.withChangeAt(Fraction.whole(1), Tempo.of(60));

        TempoMap slower = song.scaledBy(0.5);

        assertThat(slower.tempoAt(Fraction.ZERO)).isEqualTo(Tempo.of(60));
        assertThat(slower.tempoAt(Fraction.whole(1))).isEqualTo(Tempo.of(30));
        assertThat(slower.nanosAt(Fraction.whole(1))).isEqualTo(4000 * MS);
    }

    @Test
    void keepsScaledTempoWithinWhatCanBePlayed() {
        assertThat(TempoMap.constant(Tempo.of(200)).scaledBy(2).tempoAt(Fraction.ZERO)).isEqualTo(Tempo.of(240));
    }

    @Test
    void followsAnotherMapFromAPosition() {
        TempoMap song = steady.withChangeAt(Fraction.whole(2), Tempo.of(60));

        TempoMap changed = song.withSegmentsFrom(Fraction.whole(1), song.scaledBy(0.5));

        assertThat(changed.nanosAt(Fraction.whole(1))).isEqualTo(2000 * MS);
        assertThat(changed.tempoAt(Fraction.of(3, 2))).isEqualTo(Tempo.of(60));
        assertThat(changed.tempoAt(Fraction.whole(2))).isEqualTo(Tempo.of(30));
        assertThat(changed.nanosAt(Fraction.whole(2))).isEqualTo(6000 * MS);
    }
}
