package io.github.hariskov.piano.midi;

@FunctionalInterface
public interface InputListener {

    void onEvent(InputEvent event);
}
