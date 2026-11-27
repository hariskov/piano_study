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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MidiFileReader {

    private static final int TRACK_NAME = 0x03;
    private static final int SET_TEMPO = 0x51;
    private static final int TIME_SIGNATURE = 0x58;
    private static final int KEY_SIGNATURE = 0x59;

    private record Held(int track, int channel, int pitch) {
    }

    private record Started(long tick, int velocity) {
    }

    public MidiFileReader() {
    }

    public RawMidi read(Path file) throws IOException {
        String name = file.getFileName().toString();
        try {
            int type = MidiSystem.getMidiFileFormat(file.toFile()).getType();
            Sequence sequence = MidiSystem.getSequence(file.toFile());
            if (sequence.getDivisionType() != Sequence.PPQ) {
                throw new MidiImportException(name + ": SMPTE-timed MIDI files are not supported");
            }
            return read(sequence, type);
        } catch (InvalidMidiDataException e) {
            throw new MidiImportException(name + ": not a readable MIDI file (" + e.getMessage() + ")", e);
        }
    }

    private RawMidi read(Sequence sequence, int type) {
        List<RawNote> notes = new ArrayList<>();
        Map<Integer, String> trackNames = new HashMap<>();
        Map<RawMidi.TrackChannel, Integer> programs = new HashMap<>();
        List<RawTempo> tempos = new ArrayList<>();
        List<RawTimeSignature> timeSignatures = new ArrayList<>();
        List<RawKeySignature> keys = new ArrayList<>();
        Set<String> warnings = new LinkedHashSet<>();
        Map<Held, Deque<Started>> open = new HashMap<>();
        Track[] tracks = sequence.getTracks();
        for (int index = 0; index < tracks.length; index++) {
            for (int event = 0; event < tracks[index].size(); event++) {
                MidiEvent midiEvent = tracks[index].get(event);
                long tick = midiEvent.getTick();
                switch (midiEvent.getMessage()) {
                    case ShortMessage message -> shortMessage(index, tick, message, open, notes, programs);
                    case MetaMessage meta -> {
                        byte[] data = meta.getData();
                        switch (meta.getType()) {
                            case TRACK_NAME -> trackNames.putIfAbsent(index, new String(data, StandardCharsets.UTF_8).trim());
                            case SET_TEMPO -> tempos.add(new RawTempo(tick,
                                    ((data[0] & 0xFF) << 16) | ((data[1] & 0xFF) << 8) | (data[2] & 0xFF)));
                            case TIME_SIGNATURE -> timeSignatures.add(new RawTimeSignature(tick, data[0], 1 << data[1]));
                            case KEY_SIGNATURE -> keys.add(new RawKeySignature(tick, data[0], data[1] == 1));
                            default -> {
                            }
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        long end = sequence.getTickLength();
        open.forEach((held, starts) -> starts.forEach(started -> {
            notes.add(new RawNote(held.track(), held.channel(), held.pitch(), started.velocity(), started.tick(), end));
            warnings.add("Some notes were never released; they last until the end of the song");
        }));
        notes.sort(Comparator.comparingLong(RawNote::startTick).thenComparingInt(RawNote::track)
                .thenComparingInt(RawNote::pitch).thenComparingInt(RawNote::channel));
        Comparator<RawTempo> byTick = Comparator.comparingLong(RawTempo::tick);
        tempos.sort(byTick);
        timeSignatures.sort(Comparator.comparingLong(RawTimeSignature::tick));
        keys.sort(Comparator.comparingLong(RawKeySignature::tick));
        return new RawMidi(type, sequence.getResolution(), notes, trackNames, programs, tempos, timeSignatures, keys,
                end, new ArrayList<>(warnings));
    }

    private static void shortMessage(int track, long tick, ShortMessage message, Map<Held, Deque<Started>> open,
                                     List<RawNote> notes, Map<RawMidi.TrackChannel, Integer> programs) {
        int channel = message.getChannel();
        switch (message.getCommand()) {
            case ShortMessage.NOTE_ON -> {
                Held held = new Held(track, channel, message.getData1());
                if (message.getData2() > 0) {
                    open.computeIfAbsent(held, key -> new ArrayDeque<>()).addLast(new Started(tick, message.getData2()));
                } else {
                    release(held, tick, open, notes);
                }
            }
            case ShortMessage.NOTE_OFF -> release(new Held(track, channel, message.getData1()), tick, open, notes);
            case ShortMessage.PROGRAM_CHANGE -> programs.putIfAbsent(new RawMidi.TrackChannel(track, channel),
                    message.getData1());
            default -> {
            }
        }
    }

    private static void release(Held held, long tick, Map<Held, Deque<Started>> open, List<RawNote> notes) {
        Deque<Started> starts = open.get(held);
        if (starts == null || starts.isEmpty()) {
            return;
        }
        Started started = starts.removeFirst();
        if (starts.isEmpty()) {
            open.remove(held);
        }
        if (tick > started.tick()) {
            notes.add(new RawNote(held.track(), held.channel(), held.pitch(), started.velocity(), started.tick(), tick));
        }
    }
}
