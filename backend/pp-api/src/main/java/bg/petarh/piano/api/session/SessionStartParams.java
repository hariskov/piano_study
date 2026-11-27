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

    SessionStartParams adjustedBy(AdjustParams change) {
        return new SessionStartParams(input, exercise, mode, tempo, key, or(change.hands(), hands), range, keyboard,
                noteValue, time, notes, countIn, seed, metronome, or(change.level(), level),
                or(change.accidentals(), accidentals), or(change.mixed(), mixed),
                or(change.chordTypes(), chordTypes), or(change.inversions(), inversions));
    }

    private static <T> T or(T value, T fallback) {
        return value != null ? value : fallback;
    }
}
