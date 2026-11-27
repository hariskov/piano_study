package bg.petarh.piano.engine.metronome;

import bg.petarh.piano.engine.run.SessionListener;
import bg.petarh.piano.engine.session.Session;
import bg.petarh.piano.model.time.Fraction;

public final class Metronome implements SessionListener {

    private final Session session;
    private final ClickOutput output;
    private final int beatsPerMeasure;
    private long nextBeat;

    public Metronome(Session session, ClickOutput output) {
        this.session = session;
        this.output = output;
        this.beatsPerMeasure = session.timing().meter().beatsPerMeasure();
        this.nextBeat = -(long) session.timing().countInBars() * beatsPerMeasure;
    }

    @Override
    public void onPosition(long positionNanos) {
        long due = Long.MIN_VALUE;
        while (beatNanos(nextBeat) <= positionNanos) {
            due = nextBeat++;
        }
        if (due != Long.MIN_VALUE) {
            output.click(Math.floorMod(due, beatsPerMeasure) == 0);
        }
    }

    private long beatNanos(long beat) {
        Fraction position = session.timing().meter().countedBeat().times(beat);
        return session.nanosAt(position);
    }
}
