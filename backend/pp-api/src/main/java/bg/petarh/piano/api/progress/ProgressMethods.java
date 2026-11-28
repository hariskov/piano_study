package bg.petarh.piano.api.progress;

import bg.petarh.piano.api.rpc.MayBeAbsent;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.library.progress.SessionHistory;
import bg.petarh.piano.library.progress.SessionRecord;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class ProgressMethods {

    public record HistoryParams(@MayBeAbsent String songId, @MayBeAbsent String exercise) {
    }

    public record History(List<PastSession> sessions) {
    }

    public record PastSession(String finishedAt, String exercise, @MayBeAbsent String songId, @MayBeAbsent String part,
                              String hands, String mode, int tempo, @MayBeAbsent Integer tempoPercent,
                              @MayBeAbsent Integer fromBar, @MayBeAbsent Integer toBar, int accuracyPercent,
                              boolean timed, int completedTargets, int wrongKeys, int perfect, int good, int ok,
                              int missed, long elapsedMs, List<Integer> passes, Map<String, Integer> mistakesByBar) {

        static PastSession of(SessionRecord session) {
            Map<String, Integer> mistakes = new TreeMap<>();
            session.mistakesByBar().forEach((bar, count) -> mistakes.put(String.valueOf(bar), count));
            return new PastSession(session.finishedAt().toString(), session.exercise(), session.songId(),
                    session.part(), session.hands(), session.mode(), session.tempo(), session.tempoPercent(),
                    session.fromBar(), session.toBar(), session.accuracyPercent(), session.timed(),
                    session.completedTargets(), session.wrongKeys(), session.perfect(), session.good(), session.ok(),
                    session.missed(), session.elapsedMs(), session.passes(), mistakes);
        }
    }

    private ProgressMethods() {
    }

    public static void register(RpcServer server, SessionHistory history) {
        server.register("progress.history", HistoryParams.class, History.class, params -> {
            String key = params.songId() != null ? "song-" + params.songId() : params.exercise();
            if (key == null) {
                throw new IllegalArgumentException("Give a songId or an exercise (random or chords)");
            }
            return new History(history.history(key).stream().map(PastSession::of).toList());
        });
    }
}
