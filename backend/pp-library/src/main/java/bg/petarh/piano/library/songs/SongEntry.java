package bg.petarh.piano.library.songs;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record SongEntry(String id, String title, String fileName, Optional<String> midiFile, int partCount,
                        int measureCount, String sourceFile, String format, Instant importedAt, List<String> warnings) {

    public SongEntry {
        warnings = List.copyOf(warnings);
    }
}
