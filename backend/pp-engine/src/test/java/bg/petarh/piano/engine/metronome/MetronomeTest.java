package bg.petarh.piano.engine.metronome;

import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MetronomeTest {

    private static final long MS = 1_000_000L;

    private final List<Boolean> clicks = new ArrayList<>();

    @Test
    void clicksEveryBeatOfTheCountInAndAccentsTheFirst() {
        Metronome metronome = metronome(120, "4/4", 1);

        for (long ms = -2000; ms < 0; ms += 250) {
            metronome.onPosition(ms * MS);
        }

        assertThat(clicks).containsExactly(true, false, false, false);
    }

    @Test
    void keepsClickingAfterTheCountInWithAnAccentOnEachBar() {
        Metronome metronome = metronome(120, "3/4", 0);

        for (long ms = 0; ms <= 3000; ms += 100) {
            metronome.onPosition(ms * MS);
        }

        assertThat(clicks).containsExactly(true, false, false, true, false, false, true);
    }

    @Test
    void countsCompoundMetersInDottedBeats() {
        Metronome metronome = metronome(90, "6/8", 0);

        for (long ms = 0; ms <= 4000; ms += 50) {
            metronome.onPosition(ms * MS);
        }

        assertThat(clicks).containsExactly(true, false, true, false, true);
    }

    @Test
    void doesNotClickBeforeTheFirstBeat() {
        Metronome metronome = metronome(120, "4/4", 1);

        metronome.onPosition(-2001 * MS);

        assertThat(clicks).isEmpty();
    }

    @Test
    void clicksOnceWhenATickArrivesLate() {
        Metronome metronome = metronome(120, "4/4", 0);
        metronome.onPosition(0);

        metronome.onPosition(1600 * MS);

        assertThat(clicks).containsExactly(true, false);
    }

    @Test
    void neverClicksTheSameBeatTwice() {
        Metronome metronome = metronome(120, "4/4", 0);

        metronome.onPosition(0);
        metronome.onPosition(10 * MS);

        assertThat(clicks).containsExactly(true);
    }

    private Metronome metronome(int bpm, String meter, int countInBars) {
        return new Metronome(new SessionTiming(Tempo.of(bpm), TimeSignature.parse(meter), countInBars), clicks::add);
    }
}
