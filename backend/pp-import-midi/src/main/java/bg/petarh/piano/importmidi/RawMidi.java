package bg.petarh.piano.importmidi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record RawMidi(int type, int ticksPerQuarter, List<RawNote> notes, Map<Integer, String> trackNames,
                      Map<TrackChannel, Integer> programs, List<RawTempo> tempos,
                      List<RawTimeSignature> timeSignatures, List<RawKeySignature> keySignatures, long endTick,
                      List<String> warnings) {

    public record TrackChannel(int track, int channel) {
    }

    public RawMidi {
        notes = List.copyOf(notes);
        trackNames = Map.copyOf(trackNames);
        programs = Map.copyOf(programs);
        tempos = List.copyOf(tempos);
        timeSignatures = List.copyOf(timeSignatures);
        keySignatures = List.copyOf(keySignatures);
        warnings = List.copyOf(warnings);
    }

    public Optional<String> trackName(int track) {
        return Optional.ofNullable(trackNames.get(track));
    }

    public Optional<Integer> program(int track, int channel) {
        return Optional.ofNullable(programs.get(new TrackChannel(track, channel)));
    }
}
