package bg.petarh.piano.engine.metronome;

import bg.petarh.piano.engine.run.SessionListener;
import bg.petarh.piano.engine.session.SessionTiming;
import bg.petarh.piano.model.time.Fraction;

public final class Metronome implements SessionListener {

    private final SessionTiming timing;
    private final ClickOutput output;
    private final int beatsPerMeasure;
    private long nextBeat;

    public Metronome(SessionTiming timing, ClickOutput output) {
        this.timing = timing;
        this.output = output;
        this.beatsPerMeasure = timing.meter().beatsPerMeasure();
        this.nextBeat = -(long) timing.countInBars() * beatsPerMeasure;
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
        Fraction position = timing.meter().countedBeat().times(beat);
        return timing.tempo().nanosFor(position);
    }
}
