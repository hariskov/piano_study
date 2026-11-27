package bg.petarh.piano.engine.targets;

import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.time.Fraction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static bg.petarh.piano.engine.targets.TargetFixtures.QUARTER;
import static bg.petarh.piano.engine.targets.TargetFixtures.target;
import static org.assertj.core.api.Assertions.assertThat;

class ListTargetsTest {

    private final Target first = target("C4", Fraction.ZERO);
    private final Target second = target("D4", QUARTER);
    private final TargetSource source = TargetSource.of(List.of(first, second));

    @Test
    void handsOutTargetsInOrderUntilTheyRunOut() {
        assertThat(source.next()).contains(first);
        assertThat(source.next()).contains(second);
        assertThat(source.next()).isEmpty();
    }

    @Test
    void peekingDoesNotUseTargetsUp() {
        assertThat(source.peek(5)).containsExactly(first, second);
        assertThat(source.next()).contains(first);
        assertThat(source.peek(5)).containsExactly(second);
    }
}
