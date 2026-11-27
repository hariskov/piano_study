package bg.petarh.piano.engine.evaluation;

import java.time.Duration;
import java.util.Optional;

public record TimingWindows(Duration perfect, Duration good, Duration ok) {

    public static final TimingWindows DEFAULT =
            new TimingWindows(Duration.ofMillis(50), Duration.ofMillis(100), Duration.ofMillis(150));

    public TimingWindows {
        if (perfect.isNegative() || good.compareTo(perfect) < 0 || ok.compareTo(good) < 0) {
            throw new IllegalArgumentException("Timing windows must widen from perfect to ok: "
                    + perfect + ", " + good + ", " + ok);
        }
    }

    public Optional<TimingGrade> grade(long offsetNanos) {
        long distance = Math.abs(offsetNanos);
        if (distance <= perfect.toNanos()) {
            return Optional.of(TimingGrade.PERFECT);
        }
        if (distance <= good.toNanos()) {
            return Optional.of(TimingGrade.GOOD);
        }
        return distance <= ok.toNanos() ? Optional.of(TimingGrade.OK) : Optional.empty();
    }
}
