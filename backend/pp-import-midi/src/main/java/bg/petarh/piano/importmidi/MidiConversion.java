package bg.petarh.piano.importmidi;

import bg.petarh.piano.model.score.Score;

import java.util.List;

public record MidiConversion(Score score, List<String> warnings) {

    public MidiConversion {
        warnings = List.copyOf(warnings);
    }
}
