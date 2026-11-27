package bg.petarh.piano.app.practice;

import bg.petarh.piano.engine.run.Summary;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class PracticeSummaryTest {

    @Test
    void describesAWaitSession() {
        assertThat(PracticeSummary.describe(new Summary(false, 2, 3, 1, 0, 0, 0, 0, 75), Duration.ofSeconds(83)))
                .isEqualTo("Done: 2 targets, 1 wrong key, accuracy 75%, time 1:23.");
    }

    @Test
    void describesATempoSession() {
        assertThat(PracticeSummary.describe(new Summary(true, 0, 0, 2, 2, 1, 0, 1, 75), Duration.ofSeconds(5)))
                .isEqualTo("Done: perfect 2, good 1, ok 0, missed 1, wrong keys 2, accuracy 75%, time 0:05.");
    }

    @Test
    void describesASessionWithNothingPlayed() {
        assertThat(PracticeSummary.describe(new Summary(false, 0, 0, 0, 0, 0, 0, 0, 0), Duration.ZERO))
                .isEqualTo("Done: 0 targets, 0 wrong keys, accuracy 0%, time 0:00.");
    }
}
