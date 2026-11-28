package bg.petarh.piano.library.progress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProgressLogTest {

    @TempDir
    Path folder;

    @Test
    void keepsTheSessionsOfEachSongAndExerciseInTheirOwnFile() {
        ProgressLog log = new ProgressLog(folder);

        log.record(song("sonatina", 1, 70));
        log.record(song("sonatina", 2, 85));
        log.record(random(3, 90));

        assertThat(log.history("song-sonatina")).extracting(SessionRecord::accuracyPercent).containsExactly(70, 85);
        assertThat(log.history("random")).extracting(SessionRecord::accuracyPercent).containsExactly(90);
        assertThat(log.history("chords")).isEmpty();
        assertThat(folder.resolve("song-sonatina.json")).exists();
        assertThat(folder.resolve("random.json")).exists();
    }

    @Test
    void readsBackEverythingItSaved() {
        SessionRecord saved = new SessionRecord(Instant.parse("2026-11-28T10:15:30Z"), "song", "sonatina", "P1", "both",
                "tempo", 84, 70, 5, 8, 92, true, 40, 3, 20, 12, 5, 3, 61_000, List.of(80, 95),
                Map.of(6, 2, 7, 4));

        new ProgressLog(folder).record(saved);

        assertThat(new ProgressLog(folder).history("song-sonatina")).containsExactly(saved);
    }

    @Test
    void keepsOnlyTheNewestSessions() {
        ProgressLog log = new ProgressLog(folder);

        for (int i = 0; i < ProgressLog.KEPT_SESSIONS + 5; i++) {
            log.record(random(i, i % 100));
        }

        List<SessionRecord> history = log.history("random");
        assertThat(history).hasSize(ProgressLog.KEPT_SESSIONS);
        assertThat(history.getFirst().finishedAt()).isEqualTo(Instant.ofEpochSecond(5));
    }

    @Test
    void setsAnUnreadableFileAsideAndStartsAgain() throws IOException {
        Files.writeString(folder.resolve("random.json"), "{ not json");
        ProgressLog log = new ProgressLog(folder);

        assertThat(log.history("random")).isEmpty();
        log.record(random(1, 50));

        assertThat(folder.resolve("random.json.bad")).exists();
        assertThat(log.history("random")).hasSize(1);
    }

    @Test
    void refusesKeysThatAreNotPlainNames() {
        assertThatThrownBy(() -> new ProgressLog(folder).history("../settings")).hasMessageContaining("../settings");
    }

    private static SessionRecord song(String id, long second, int accuracy) {
        return new SessionRecord(Instant.ofEpochSecond(second), "song", id, "P1", "right", "wait", 90, 100, null, null,
                accuracy, false, 30, 2, 0, 0, 0, 0, 40_000, List.of(), Map.of());
    }

    private static SessionRecord random(long second, int accuracy) {
        return new SessionRecord(Instant.ofEpochSecond(second), "random", null, null, "right", "wait", 90, null, null,
                null, accuracy, false, 50, 1, 0, 0, 0, 0, 30_000, List.of(), Map.of());
    }
}
