package bg.petarh.piano.importmidi;

import bg.petarh.piano.model.score.Score;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record MidiConversion(Score score, List<String> warnings, Map<String, String> instruments) {

    public MidiConversion {
        warnings = List.copyOf(warnings);
        instruments = Collections.unmodifiableMap(new LinkedHashMap<>(instruments));
    }
}
