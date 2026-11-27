package bg.petarh.piano.api.session;

import java.util.List;

public record AdjustParams(String hands, Integer level, Boolean accidentals, Boolean mixed, List<String> chordTypes,
                           List<Integer> inversions) {
}
