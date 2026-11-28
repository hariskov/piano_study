package bg.petarh.piano.library.progress;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SessionRecord(Instant finishedAt, String exercise, String songId, String part, String hands, String mode,
                            int tempo, Integer tempoPercent, Integer fromBar, Integer toBar, int accuracyPercent,
                            boolean timed, int completedTargets, int wrongKeys, int perfect, int good, int ok,
                            int missed, long elapsedMs, List<Integer> passes, Map<Integer, Integer> mistakesByBar) {

    public SessionRecord {
        passes = passes == null ? List.of() : List.copyOf(passes);
        mistakesByBar = mistakesByBar == null ? Map.of() : Map.copyOf(mistakesByBar);
    }

    public String key() {
        return songId == null ? exercise : "song-" + songId;
    }
}
