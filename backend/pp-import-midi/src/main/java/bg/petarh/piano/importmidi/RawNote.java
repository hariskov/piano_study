package bg.petarh.piano.importmidi;

public record RawNote(int track, int channel, int pitch, int velocity, long startTick, long endTick) {
}
