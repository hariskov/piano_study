package bg.petarh.piano.midi.input;

@FunctionalInterface
public interface InputListener {

    void onEvent(InputEvent event);
}
