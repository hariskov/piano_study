package io.github.hariskov.piano.midi.input;

@FunctionalInterface
public interface InputListener {

    void onEvent(InputEvent event);
}
