package bg.petarh.piano.api.session;

import java.util.List;

public record SessionStartParams(
        String input,
        String exercise,
        String mode,
        Integer tempo,
        String key,
        String hands,
        String range,
        String keyboard,
        String noteValue,
        String time,
        Integer notes,
        Integer countIn,
        Long seed,
        String metronome,
        Integer level,
        Boolean accidentals,
        Boolean mixed,
        List<String> chordTypes,
        List<Integer> inversions) {
}
