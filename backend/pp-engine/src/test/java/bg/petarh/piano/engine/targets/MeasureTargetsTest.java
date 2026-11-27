package bg.petarh.piano.engine.targets;

import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static bg.petarh.piano.engine.targets.TargetFixtures.note;
import static org.assertj.core.api.Assertions.assertThat;

class MeasureTargetsTest {

    private final CountingMeasures measures = new CountingMeasures();

    @Test
    void readsFourMeasuresAheadBeforeTheFirstTarget() {
        TargetSource source = TargetSource.fromMeasures(measures);

        assertThat(source.next()).isPresent();
        assertThat(measures.supplied).isEqualTo(5);
    }

    @Test
    void keepsFourMeasuresAheadAsTargetsAreUsed() {
        TargetSource source = TargetSource.fromMeasures(measures);

        for (int i = 0; i < 4; i++) {
            source.next();
        }
        assertThat(measures.supplied).isEqualTo(5);

        source.next();
        assertThat(measures.supplied).isEqualTo(6);
    }

    @Test
    void groupsNotesThatStartTogetherIntoOneTarget() {
        TargetSource source = TargetSource.fromMeasures(() -> List.of(
                note("C4", Fraction.ZERO), note("E4", Fraction.ZERO), note("G4", Fraction.of(1, 2))));

        assertThat(source.next()).get().extracting(Target::isChord).isEqualTo(true);
        assertThat(source.next()).get().extracting(Target::isChord).isEqualTo(false);
    }

    @Test
    void peeksAcrossMeasures() {
        TargetSource source = TargetSource.fromMeasures(measures);

        assertThat(source.peek(6)).extracting(Target::start).containsExactly(
                Fraction.ZERO, Fraction.of(1, 4), Fraction.of(1, 2), Fraction.of(3, 4),
                Fraction.whole(1), Fraction.of(5, 4));
        assertThat(source.next()).get().extracting(Target::start).isEqualTo(Fraction.ZERO);
    }

    @Test
    void neverRunsOut() {
        TargetSource source = TargetSource.fromMeasures(measures);

        for (int i = 0; i < 1000; i++) {
            assertThat(source.next()).isPresent();
        }
    }

    private static final class CountingMeasures implements Supplier<List<NoteEvent>> {

        private int supplied;

        @Override
        public List<NoteEvent> get() {
            Fraction start = Fraction.whole(supplied++);
            return List.of(
                    note("C4", start),
                    note("D4", start.plus(Fraction.of(1, 4))),
                    note("E4", start.plus(Fraction.of(1, 2))),
                    note("F4", start.plus(Fraction.of(3, 4))));
        }
    }
}
