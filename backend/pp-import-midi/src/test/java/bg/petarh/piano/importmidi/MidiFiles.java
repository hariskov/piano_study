package bg.petarh.piano.importmidi;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

final class MidiFiles {

    static final int PPQ = 480;

    private final Sequence sequence;

    private MidiFiles() {
        try {
            sequence = new Sequence(Sequence.PPQ, PPQ);
        } catch (InvalidMidiDataException e) {
            throw new IllegalStateException(e);
        }
    }

    static MidiFiles create() {
        return new MidiFiles();
    }

    Track track() {
        return sequence.createTrack();
    }

    MidiFiles name(Track track, String name) {
        meta(track, 0x03, name.getBytes(StandardCharsets.UTF_8), 0);
        return this;
    }

    MidiFiles program(Track track, int channel, int program, long tick) {
        shortMessage(track, ShortMessage.PROGRAM_CHANGE, channel, program, 0, tick);
        return this;
    }

    MidiFiles note(Track track, int channel, int pitch, long onTick, long offTick) {
        shortMessage(track, ShortMessage.NOTE_ON, channel, pitch, 80, onTick);
        shortMessage(track, ShortMessage.NOTE_OFF, channel, pitch, 0, offTick);
        return this;
    }

    MidiFiles noteOffAsZeroVelocity(Track track, int channel, int pitch, long onTick, long offTick) {
        shortMessage(track, ShortMessage.NOTE_ON, channel, pitch, 80, onTick);
        shortMessage(track, ShortMessage.NOTE_ON, channel, pitch, 0, offTick);
        return this;
    }

    MidiFiles noteOn(Track track, int channel, int pitch, long tick) {
        shortMessage(track, ShortMessage.NOTE_ON, channel, pitch, 80, tick);
        return this;
    }

    MidiFiles tempo(Track track, int microsPerQuarter, long tick) {
        meta(track, 0x51, new byte[]{(byte) (microsPerQuarter >> 16), (byte) (microsPerQuarter >> 8),
                (byte) microsPerQuarter}, tick);
        return this;
    }

    MidiFiles timeSignature(Track track, int numerator, int denominatorPower, long tick) {
        meta(track, 0x58, new byte[]{(byte) numerator, (byte) denominatorPower, 24, 8}, tick);
        return this;
    }

    MidiFiles keySignature(Track track, int sharpsOrFlats, boolean minor, long tick) {
        meta(track, 0x59, new byte[]{(byte) sharpsOrFlats, (byte) (minor ? 1 : 0)}, tick);
        return this;
    }

    Path write(Path file, int type) throws IOException {
        MidiSystem.write(sequence, type, file.toFile());
        return file;
    }

    private void shortMessage(Track track, int command, int channel, int data1, int data2, long tick) {
        try {
            track.add(new MidiEvent(new ShortMessage(command, channel, data1, data2), tick));
        } catch (InvalidMidiDataException e) {
            throw new IllegalStateException(e);
        }
    }

    private void meta(Track track, int type, byte[] data, long tick) {
        try {
            track.add(new MidiEvent(new MetaMessage(type, data, data.length), tick));
        } catch (InvalidMidiDataException e) {
            throw new IllegalStateException(e);
        }
    }
}
