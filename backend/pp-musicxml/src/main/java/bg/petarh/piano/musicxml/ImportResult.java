package bg.petarh.piano.musicxml;

import bg.petarh.piano.model.score.Score;

import java.util.List;

public record ImportResult(Score score, List<String> warnings) {

    public ImportResult {
        warnings = List.copyOf(warnings);
    }
}
