package bg.petarh.piano.engine.evaluation;

import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.engine.time.ManualClock;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FollowModeTest {

    private final Session session = new Session(TargetSource.of(List.of()),
            new SessionTiming(Tempo.of(90), TimeSignature.COMMON_TIME, 0), new ManualClock());

    @Test
    void choosesTheEvaluatorForEachMode() {
        assertThat(FollowMode.WAIT.evaluatorFor(session)).isInstanceOf(WaitEvaluator.class);
        assertThat(FollowMode.TEMPO.evaluatorFor(session)).isInstanceOf(TempoEvaluator.class);
    }
}
