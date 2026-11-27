package bg.petarh.piano.importmidi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sound.midi.Track;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static bg.petarh.piano.importmidi.MidiFiles.PPQ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MidiFileReaderTest {

    @TempDir
    Path folder;

    private final MidiFileReader reader = new MidiFileReader();

    @Test
    void readsNotesOfEveryTrackAndChannel() throws IOException {
        MidiFiles midi = MidiFiles.create();
        Track conductor = midi.track();
        midi.tempo(conductor, 500_000, 0).timeSignature(conductor, 3, 2, 0).keySignature(conductor, -2, false, 0);
        Track right = midi.track();
        midi.name(right, "Right hand").program(right, 0, 0, 0)
                .note(right, 0, 72, 0, PPQ).noteOffAsZeroVelocity(right, 0, 76, PPQ, 2L * PPQ);
        Track strings = midi.track();
        midi.name(strings, "Strings").program(strings, 3, 48, 0).note(strings, 3, 55, 0, 3L * PPQ);

        RawMidi raw = reader.read(midi.write(folder.resolve("song.mid"), 1));

        assertThat(raw.type()).isEqualTo(1);
        assertThat(raw.ticksPerQuarter()).isEqualTo(PPQ);
        assertThat(raw.notes()).containsExactly(
                new RawNote(1, 0, 72, 80, 0, PPQ),
                new RawNote(2, 3, 55, 80, 0, 3L * PPQ),
                new RawNote(1, 0, 76, 80, PPQ, 2L * PPQ));
        assertThat(raw.trackName(1)).contains("Right hand");
        assertThat(raw.program(2, 3)).contains(48);
        assertThat(raw.program(1, 5)).isEmpty();
        assertThat(raw.tempos()).containsExactly(new RawTempo(0, 500_000));
        assertThat(raw.timeSignatures()).containsExactly(new RawTimeSignature(0, 3, 4));
        assertThat(raw.keySignatures()).containsExactly(new RawKeySignature(0, -2, false));
        assertThat(raw.endTick()).isEqualTo(3L * PPQ);
    }

    @Test
    void readsTypeZeroFilesWithManyChannelsInOneTrack() throws IOException {
        MidiFiles midi = MidiFiles.create();
        Track only = midi.track();
        midi.note(only, 0, 60, 0, PPQ).note(only, 1, 48, 0, PPQ).note(only, 9, 36, 0, PPQ / 2);

        RawMidi raw = reader.read(midi.write(folder.resolve("type0.mid"), 0));

        assertThat(raw.type()).isZero();
        assertThat(raw.notes()).extracting(RawNote::channel).containsExactlyInAnyOrder(0, 1, 9);
    }

    @Test
    void pairsRepeatedNotesInOrder() throws IOException {
        MidiFiles midi = MidiFiles.create();
        Track track = midi.track();
        midi.noteOn(track, 0, 60, 0).noteOn(track, 0, 60, PPQ / 2).note(track, 0, 62, 0, PPQ);
        midi.note(track, 0, 60, 2L * PPQ, 3L * PPQ);

        RawMidi raw = reader.read(midi.write(folder.resolve("repeat.mid"), 1));

        assertThat(raw.notes()).filteredOn(note -> note.pitch() == 60)
                .extracting(RawNote::startTick).contains(0L, (long) PPQ / 2, 2L * PPQ);
    }

    @Test
    void endsNotesThatAreNeverReleasedAtTheEndAndSaysSo() throws IOException {
        MidiFiles midi = MidiFiles.create();
        Track track = midi.track();
        midi.noteOn(track, 0, 64, 0).note(track, 0, 60, 0, 4L * PPQ);

        RawMidi raw = reader.read(midi.write(folder.resolve("stuck.mid"), 1));

        assertThat(raw.notes()).filteredOn(note -> note.pitch() == 64).extracting(RawNote::endTick)
                .containsExactly(4L * PPQ);
        assertThat(raw.warnings()).anySatisfy(warning -> assertThat(warning).contains("never released"));
    }

    @Test
    void refusesFilesThatAreNotMidi() throws IOException {
        Path text = Files.writeString(folder.resolve("song.mid"), "not a midi file");

        assertThatThrownBy(() -> reader.read(text)).isInstanceOf(MidiImportException.class).hasMessageContaining("song.mid");
    }
}
