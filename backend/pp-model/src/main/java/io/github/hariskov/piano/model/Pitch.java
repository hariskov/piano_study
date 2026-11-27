package io.github.hariskov.piano.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record Pitch(int midiNumber) implements Comparable<Pitch> {

    private static final int LOWEST_MIDI_NUMBER = 0;
    private static final int HIGHEST_MIDI_NUMBER = 127;
    private static final int SEMITONES_PER_OCTAVE = 12;
    private static final String[] SHARP_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final int[] NATURAL_OFFSETS = {9, 11, 0, 2, 4, 5, 7};
    private static final Pattern NOTE_NAME = Pattern.compile("([A-Ga-g])([#b]?)(-1|[0-9])");

    public Pitch {
        if (midiNumber < LOWEST_MIDI_NUMBER || midiNumber > HIGHEST_MIDI_NUMBER) {
            throw new IllegalArgumentException("MIDI number out of range 0-127: " + midiNumber);
        }
    }

    public static Pitch of(int midiNumber) {
        return new Pitch(midiNumber);
    }

    public static Pitch parse(String name) {
        Matcher matcher = NOTE_NAME.matcher(name);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid note name: " + name);
        }
        int natural = NATURAL_OFFSETS[Character.toUpperCase(matcher.group(1).charAt(0)) - 'A'];
        int accidental = switch (matcher.group(2)) {
            case "#" -> 1;
            case "b" -> -1;
            default -> 0;
        };
        int octave = Integer.parseInt(matcher.group(3));
        int midiNumber = (octave + 1) * SEMITONES_PER_OCTAVE + natural + accidental;
        if (midiNumber < LOWEST_MIDI_NUMBER || midiNumber > HIGHEST_MIDI_NUMBER) {
            throw new IllegalArgumentException("Note name outside MIDI range: " + name);
        }
        return new Pitch(midiNumber);
    }

    public int pitchClass() {
        return midiNumber % SEMITONES_PER_OCTAVE;
    }

    public int octave() {
        return midiNumber / SEMITONES_PER_OCTAVE - 1;
    }

    public String name() {
        return SHARP_NAMES[pitchClass()] + octave();
    }

    public boolean isBlackKey() {
        return SHARP_NAMES[pitchClass()].length() > 1;
    }

    public Pitch transpose(int semitones) {
        return new Pitch(midiNumber + semitones);
    }

    @Override
    public int compareTo(Pitch other) {
        return Integer.compare(midiNumber, other.midiNumber);
    }

    @Override
    public String toString() {
        return name();
    }
}
