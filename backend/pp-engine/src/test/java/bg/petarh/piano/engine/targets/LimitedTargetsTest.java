package bg.petarh.piano.engine.targets;

import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static bg.petarh.piano.engine.targets.TargetFixtures.QUARTER;
import static bg.petarh.piano.engine.targets.TargetFixtures.target;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class LimitedTargetsTest {

    private final TargetSource source = TargetSource.of(List.of(
            target("C4", Fraction.ZERO), target("D4", QUARTER), target("E4", Fraction.of(1, 2))));

    @Test
    void stopsAfterTheLimit() {
        TargetSource limited = source.limitedTo(2);

        assertThat(limited.next()).isPresent();
        assertThat(limited.next()).isPresent();
        assertThat(limited.next()).isEmpty();
    }

    @Test
    void peeksNoFurtherThanTheLimit() {
        TargetSource limited = source.limitedTo(2);
        limited.next();

        assertThat(limited.peek(5)).extracting(target -> target.pitches().first().name()).containsExactly("D4");
    }

    @Test
    void endsAtTheSourceWhenItIsShorter() {
        TargetSource limited = source.limitedTo(10);

        assertThat(limited.peek(10)).hasSize(3);
    }

    @Test
    void rejectsLimitBelowOne() {
        assertThatIllegalArgumentException().isThrownBy(() -> source.limitedTo(0));
    }
}
