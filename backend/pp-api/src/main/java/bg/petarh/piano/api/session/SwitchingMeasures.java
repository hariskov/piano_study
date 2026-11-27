package bg.petarh.piano.api.session;

import bg.petarh.piano.model.note.NoteEvent;

import java.util.List;
import java.util.function.Supplier;

final class SwitchingMeasures implements Supplier<List<NoteEvent>> {

    private volatile Supplier<List<NoteEvent>> current;

    SwitchingMeasures(Supplier<List<NoteEvent>> first) {
        this.current = first;
    }

    void switchTo(Supplier<List<NoteEvent>> next) {
        current = next;
    }

    @Override
    public List<NoteEvent> get() {
        return current.get();
    }
}
