package bg.petarh.piano.library.songs;

import java.time.Instant;
import java.util.List;

record MetaFile(String title, int partCount, int measureCount, String sourceFile, String format, String importedAt,
                List<String> warnings) {

    static MetaFile of(SongEntry entry) {
        return new MetaFile(entry.title(), entry.partCount(), entry.measureCount(), entry.sourceFile(), entry.format(),
                entry.importedAt().toString(), entry.warnings());
    }

    SongEntry toEntry(String id) {
        return new SongEntry(id, title, partCount, measureCount, sourceFile, format, Instant.parse(importedAt),
                warnings == null ? List.of() : warnings);
    }
}
