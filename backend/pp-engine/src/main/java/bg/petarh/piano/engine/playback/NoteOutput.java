package bg.petarh.piano.engine.playback;

import bg.petarh.piano.model.pitch.Pitch;

public interface NoteOutput {

    void noteOn(Pitch pitch);

    void noteOff(Pitch pitch);

    void allOff();
}
