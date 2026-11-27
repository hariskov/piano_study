package bg.petarh.piano.engine.metronome;

@FunctionalInterface
public interface ClickOutput {

    void click(boolean accent);
}
