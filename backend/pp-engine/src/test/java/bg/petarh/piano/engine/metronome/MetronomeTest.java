package bg.petarh.piano.engine.metronome;

import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.engine.targets.TargetSource;
import bg.petarh.piano.engine.time.ManualClock;
import bg.petarh.piano.model.note.Hand;
import bg.petarh.piano.model.note.NoteEvent;
import bg.petarh.piano.model.note.Target;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;
import org.junit.jupiter.api.Test;

import java.time.Duration;
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

    @Test
    void followsATempoChange() {
        ManualClock clock = new ManualClock();
        Session session = session(clock, 120, "4/4", 0);
        Metronome metronome = new Metronome(session, clicks::add);
        session.start();
        metronome.onPosition(0);
        clock.advance(Duration.ofMillis(100));
        session.changeTempo(Tempo.of(60));

        metronome.onPosition(500 * MS);
        metronome.onPosition(1499 * MS);
        assertThat(clicks).containsExactly(true, false);

        metronome.onPosition(1500 * MS);
        assertThat(clicks).containsExactly(true, false, false);
    }

    private Metronome metronome(int bpm, String meter, int countInBars) {
        return new Metronome(session(new ManualClock(), bpm, meter, countInBars), clicks::add);
    }

    private static Session session(ManualClock clock, int bpm, String meter, int countInBars) {
        Target note = Target.of(List.of(new NoteEvent(Pitch.parse("C4"), Fraction.ZERO, Fraction.of(1, 4), Hand.RIGHT)));
        return new Session(TargetSource.of(List.of(note)),
                new SessionTiming(Tempo.of(bpm), TimeSignature.parse(meter), countInBars), clock);
    }
}
