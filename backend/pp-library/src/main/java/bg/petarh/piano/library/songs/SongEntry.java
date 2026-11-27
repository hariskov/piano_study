package bg.petarh.piano.library.songs;

import java.time.Instant;
import java.util.List;

public record SongEntry(String id, String title, int partCount, int measureCount, String sourceFile, String format,
                        Instant importedAt, List<String> warnings) {

    public SongEntry {
        warnings = List.copyOf(warnings);
    }
}
