package bg.petarh.piano.engine.playback;

import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.engine.session.SessionState;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PlaybackSchedulerTest {

    private static final long MS = 1_000_000L;
    private static final Fraction QUARTER = Fraction.of(1, 4);

    private final ManualClock clock = new ManualClock();
    private final List<String> played = new ArrayList<>();
    private final NoteOutput output = new NoteOutput() {
        @Override
        public void noteOn(Pitch pitch) {
            played.add("on " + pitch);
        }

        @Override
        public void noteOff(Pitch pitch) {
            played.add("off " + pitch);
        }

        @Override
        public void allOff() {
            played.add("all off");
        }
    };
    private final List<NoteEvent> accompaniment = List.of(
            note("C3", Fraction.ZERO, Fraction.of(1, 2)),
            note("G3", Fraction.of(1, 2), QUARTER));

    @Test
    void playsNotesAtTheirTimeAndReleasesThemAtTheirEnd() {
        Session session = session();
        PlaybackScheduler scheduler = new PlaybackScheduler(session, accompaniment, output, false);

        scheduler.onPosition(0);
        scheduler.onPosition(1999 * MS);
        assertThat(played).containsExactly("on C3");

        scheduler.onPosition(2000 * MS);
        assertThat(played).containsExactly("on C3", "off C3", "on G3");

        scheduler.onPosition(3000 * MS);
        assertThat(played).containsExactly("on C3", "off C3", "on G3", "off G3");
    }

    @Test
    void waitsForTheStudentInWaitMode() {
        Session session = session();
        PlaybackScheduler scheduler = new PlaybackScheduler(session, accompaniment, output, true);

        scheduler.onPosition(5000 * MS);
        scheduler.onTargetChanged(Optional.of(target("E4", Fraction.ZERO)));
        assertThat(played).isEmpty();

        scheduler.onTargetChanged(Optional.of(target("F4", Fraction.of(1, 2))));
        assertThat(played).containsExactly("on C3");

        scheduler.onTargetChanged(Optional.of(target("A4", Fraction.of(3, 4))));
        assertThat(played).containsExactly("on C3", "off C3", "on G3");
    }

    @Test
    void playsWhatIsLeftWhenTheLastTargetIsDone() {
        PlaybackScheduler scheduler = new PlaybackScheduler(session(), accompaniment, output, true);
        scheduler.onTargetChanged(Optional.of(target("E4", Fraction.ZERO)));

        scheduler.onTargetChanged(Optional.empty());

        assertThat(played).containsExactly("on C3", "on G3");
    }

    @Test
    void silencesEverythingWhenPausedOrFinished() {
        PlaybackScheduler scheduler = new PlaybackScheduler(session(), accompaniment, output, false);
        scheduler.onPosition(0);

        scheduler.onStateChanged(SessionState.PAUSED);
        scheduler.onStateChanged(SessionState.FINISHED);

        assertThat(played).containsExactly("on C3", "all off", "all off");
    }

    @Test
    void picksUpAgainAfterAPause() {
        PlaybackScheduler scheduler = new PlaybackScheduler(session(), accompaniment, output, false);
        scheduler.onPosition(0);
        scheduler.onStateChanged(SessionState.PAUSED);

        scheduler.onPosition(2100 * MS);

        assertThat(played).containsExactly("on C3", "all off", "on G3");
    }

    private Session session() {
        Session session = new Session(TargetSource.of(List.of(target("E4", Fraction.ZERO))),
                new SessionTiming(Tempo.of(60), TimeSignature.COMMON_TIME, 0), clock);
        session.start();
        return session;
    }

    private static Target target(String pitch, Fraction start) {
        return Target.of(List.of(note(pitch, start, QUARTER)));
    }

    private static NoteEvent note(String pitch, Fraction start, Fraction duration) {
        return new NoteEvent(Pitch.parse(pitch), start, duration, Hand.LEFT);
    }
}
