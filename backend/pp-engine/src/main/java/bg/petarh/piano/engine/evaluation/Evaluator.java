package bg.petarh.piano.engine.evaluation;

import bg.petarh.piano.midi.input.InputEvent;

import java.util.List;

public interface Evaluator {

    List<Evaluation> onInput(InputEvent event);

    default List<Evaluation> onTick(long nowNanos) {
        return List.of();
    }
}
