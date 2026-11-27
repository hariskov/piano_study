package bg.petarh.piano.library.songs;

import bg.petarh.piano.model.score.Score;

public record LoadedSong(SongEntry entry, Score score) {
}
