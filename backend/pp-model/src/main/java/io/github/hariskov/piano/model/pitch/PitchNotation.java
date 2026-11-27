package io.github.hariskov.piano.model.pitch;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PitchNotation {

    private static final String[] SHARP_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final int[] NATURAL_OFFSETS_FROM_A = {9, 11, 0, 2, 4, 5, 7};
    private static final Pattern NOTE_NAME = Pattern.compile("([A-Ga-g])([#b]?)(-1|[0-9])");

    private PitchNotation() {
    }

    static int toMidiNumber(String name) {
        Matcher matcher = NOTE_NAME.matcher(name);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid note name: " + name);
        }
        int natural = NATURAL_OFFSETS_FROM_A[Character.toUpperCase(matcher.group(1).charAt(0)) - 'A'];
        int accidental = switch (matcher.group(2)) {
            case "#" -> 1;
            case "b" -> -1;
            default -> 0;
        };
        int octave = Integer.parseInt(matcher.group(3));
        int midiNumber = (octave + 1) * Pitch.SEMITONES_PER_OCTAVE + natural + accidental;
        if (midiNumber < Pitch.LOWEST_MIDI_NUMBER || midiNumber > Pitch.HIGHEST_MIDI_NUMBER) {
            throw new IllegalArgumentException("Note name outside MIDI range: " + name);
        }
        return midiNumber;
    }

    static String sharpName(int pitchClass) {
        return SHARP_NAMES[pitchClass];
    }

    static boolean isAccidental(int pitchClass) {
        return SHARP_NAMES[pitchClass].length() > 1;
    }
}
