package bg.petarh.piano.library.songs;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

record Index(List<Song> songs) {

    record Song(String id, String title, String file, String midiFile, int partCount, int measureCount,
                String sourceFile, String format, String importedAt, List<String> warnings) {

        static Song of(SongEntry entry) {
            return new Song(entry.id(), entry.title(), entry.fileName(), entry.midiFile().orElse(null),
                    entry.partCount(), entry.measureCount(), entry.sourceFile(), entry.format(),
                    entry.importedAt().toString(), entry.warnings());
        }

        SongEntry toEntry() {
            return new SongEntry(id, title, file, Optional.ofNullable(midiFile), partCount, measureCount, sourceFile,
                    format, Instant.parse(importedAt), warnings == null ? List.of() : warnings);
        }
    }

    Index {
        songs = songs == null ? List.of() : List.copyOf(songs);
    }
}
