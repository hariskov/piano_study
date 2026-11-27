package bg.petarh.piano.musicxml;

import bg.petarh.piano.model.key.KeyMode;
import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.ScoreNote;
import bg.petarh.piano.model.score.Spelling;
import bg.petarh.piano.model.score.Tie;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class PartReader {

    private static final int MOST_STAVES = 2;
    private static final Map<String, Fraction> BEAT_UNITS = Map.of(
            "whole", Fraction.whole(1), "half", Fraction.of(1, 2), "quarter", Fraction.of(1, 4),
            "eighth", Fraction.of(1, 8), "16th", Fraction.of(1, 16));

    private final String id;
    private final String name;
    private final ImportReport report;
    private final TreeMap<Fraction, TimeSignature> timeSignatures;
    private final TreeMap<Fraction, KeySignature> keys;
    private final TreeMap<Fraction, Tempo> tempos;
    private final List<ScoreNote> notes = new ArrayList<>();
    private final List<Fraction> measureStarts = new ArrayList<>();
    private int divisions = 1;
    private int staffCount = 1;
    private Fraction measureStart = Fraction.ZERO;
    private Fraction position = Fraction.ZERO;
    private Fraction furthest = Fraction.ZERO;
    private Fraction lastNoteStart = Fraction.ZERO;

    PartReader(String id, String name, ImportReport report, TreeMap<Fraction, TimeSignature> timeSignatures,
               TreeMap<Fraction, KeySignature> keys, TreeMap<Fraction, Tempo> tempos) {
        this.id = id;
        this.name = name;
        this.report = report;
        this.timeSignatures = timeSignatures;
        this.keys = keys;
        this.tempos = tempos;
    }

    void read(XMLStreamReader xml) throws XMLStreamException {
        Xml.children(xml, child -> {
            if (child.equals("measure")) {
                measure(xml);
            }
        });
    }

    Part part() {
        return new Part(id, name, staffCount, notes);
    }

    Fraction end() {
        return measureStart;
    }

    List<Fraction> measureStarts() {
        return List.copyOf(measureStarts);
    }

    private void measure(XMLStreamReader xml) throws XMLStreamException {
        measureStarts.add(measureStart);
        position = measureStart;
        furthest = measureStart;
        Xml.children(xml, child -> {
            switch (child) {
                case "attributes" -> attributes(xml);
                case "note" -> note(xml);
                case "backup" -> position = position.minus(duration(xml));
                case "forward" -> advance(duration(xml));
                case "direction" -> direction(xml);
                case "sound" -> sound(xml);
                case "barline" -> barline(xml);
                default -> Xml.skip(xml);
            }
        });
        Fraction length = furthest.minus(measureStart);
        measureStart = length.isPositive()
                ? furthest
                : measureStart.plus(timeSignatureAt(measureStart).measureLength());
    }

    private void attributes(XMLStreamReader xml) throws XMLStreamException {
        Xml.children(xml, child -> {
            switch (child) {
                case "divisions" -> divisions = Integer.parseInt(Xml.text(xml));
                case "staves" -> staves(Integer.parseInt(Xml.text(xml)));
                case "key" -> key(xml);
                case "time" -> time(xml);
                default -> Xml.skip(xml);
            }
        });
    }

    private void staves(int count) {
        if (count > MOST_STAVES) {
            report.warn("Part " + name + " has " + count + " staves; only the first two are played");
        }
        staffCount = Math.min(count, MOST_STAVES);
    }

    private void key(XMLStreamReader xml) throws XMLStreamException {
        int[] fifths = {0};
        boolean[] minor = {false};
        Xml.children(xml, child -> {
            switch (child) {
                case "fifths" -> fifths[0] = Integer.parseInt(Xml.text(xml));
                case "mode" -> minor[0] = Xml.text(xml).equals("minor");
                default -> Xml.skip(xml);
            }
        });
        try {
            keys.put(position, KeySignature.of(fifths[0], minor[0] ? KeyMode.MINOR : KeyMode.MAJOR));
        } catch (IllegalArgumentException e) {
            report.warn("A key signature with " + fifths[0] + " sharps or flats was ignored");
        }
    }

    private void time(XMLStreamReader xml) throws XMLStreamException {
        String[] parts = {"", ""};
        Xml.children(xml, child -> {
            switch (child) {
                case "beats" -> parts[0] = Xml.text(xml);
                case "beat-type" -> parts[1] = Xml.text(xml);
                default -> Xml.skip(xml);
            }
        });
        try {
            timeSignatures.put(position, TimeSignature.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
        } catch (IllegalArgumentException e) {
            report.warn("The time signature " + parts[0] + "/" + parts[1] + " is not supported and was ignored");
        }
    }

    private void note(XMLStreamReader xml) throws XMLStreamException {
        NoteElement note = new NoteElement();
        Xml.children(xml, child -> {
            switch (child) {
                case "grace" -> note.grace = true;
                case "cue" -> note.cue = true;
                case "chord" -> note.chord = true;
                case "rest" -> note.rest = true;
                case "unpitched" -> note.unpitched = true;
                case "pitch" -> pitch(xml, note);
                case "duration" -> note.duration = Integer.parseInt(Xml.text(xml));
                case "voice" -> note.voice = parseVoice(Xml.text(xml));
                case "staff" -> note.staff = Integer.parseInt(Xml.text(xml));
                case "tie" -> note.ties.add(xml.getAttributeValue(null, "type").equals("stop") ? Tie.STOP : Tie.START);
                default -> Xml.skip(xml);
            }
        });
        if (note.grace) {
            report.warn("Grace notes are not played; they were skipped");
            return;
        }
        if (note.cue) {
            return;
        }
        Fraction duration = Fraction.of(note.duration, divisions * 4L);
        Fraction start = note.chord ? lastNoteStart : position;
        if (note.unpitched) {
            report.warn("Part " + name + " has unpitched (percussion) notes; they were skipped");
        } else if (!note.rest && note.step != 0) {
            addNote(note, start, duration);
        }
        if (!note.chord) {
            lastNoteStart = position;
            advance(duration);
        }
    }

    private void addNote(NoteElement note, Fraction start, Fraction duration) {
        if (note.staff > MOST_STAVES) {
            return;
        }
        if (note.alter != Math.rint(note.alter)) {
            report.warn("Microtones are not supported; notes were played without the quarter tone");
        }
        try {
            Spelling spelling = Spelling.of(note.step, (int) note.alter, note.octave);
            spelling.pitch();
            notes.add(new ScoreNote(spelling, start, duration, note.staff, note.voice, tieOf(note.ties)));
        } catch (IllegalArgumentException e) {
            report.warn("A note that can't be played was skipped: " + e.getMessage());
        }
    }

    private static void pitch(XMLStreamReader xml, NoteElement note) throws XMLStreamException {
        Xml.children(xml, child -> {
            switch (child) {
                case "step" -> note.step = Xml.text(xml).charAt(0);
                case "alter" -> note.alter = Double.parseDouble(Xml.text(xml));
                case "octave" -> note.octave = Integer.parseInt(Xml.text(xml));
                default -> Xml.skip(xml);
            }
        });
    }

    private void direction(XMLStreamReader xml) throws XMLStreamException {
        Tempo[] metronome = {null};
        boolean[] sounded = {false};
        Xml.children(xml, child -> {
            switch (child) {
                case "sound" -> sounded[0] = sound(xml);
                case "direction-type" -> Xml.children(xml, type -> {
                    if (type.equals("metronome")) {
                        metronome[0] = metronome(xml);
                    } else {
                        Xml.skip(xml);
                    }
                });
                default -> Xml.skip(xml);
            }
        });
        if (!sounded[0] && metronome[0] != null) {
            tempos.put(position, metronome[0]);
        }
    }

    private boolean sound(XMLStreamReader xml) throws XMLStreamException {
        String tempo = xml.getAttributeValue(null, "tempo");
        Xml.skip(xml);
        if (tempo == null) {
            return false;
        }
        tempos.put(position, Tempo.of(clampedBpm(Double.parseDouble(tempo))));
        return true;
    }

    private Tempo metronome(XMLStreamReader xml) throws XMLStreamException {
        Fraction[] unit = {Fraction.of(1, 4)};
        boolean[] dotted = {false};
        double[] perMinute = {0};
        Xml.children(xml, child -> {
            switch (child) {
                case "beat-unit" -> unit[0] = BEAT_UNITS.getOrDefault(Xml.text(xml), Fraction.of(1, 4));
                case "beat-unit-dot" -> {
                    dotted[0] = true;
                    Xml.skip(xml);
                }
                case "per-minute" -> perMinute[0] = parseLeadingNumber(Xml.text(xml));
                default -> Xml.skip(xml);
            }
        });
        Fraction beat = dotted[0] ? unit[0].times(Fraction.of(3, 2)) : unit[0];
        return perMinute[0] > 0 ? Tempo.of(clampedBpm(perMinute[0]), beat) : null;
    }

    private void barline(XMLStreamReader xml) throws XMLStreamException {
        Xml.children(xml, child -> {
            if (child.equals("repeat")) {
                report.warn("Repeats are played straight through");
            }
            Xml.skip(xml);
        });
    }

    private int clampedBpm(double bpm) {
        long rounded = Math.round(bpm);
        int clamped = Math.clamp(rounded, Tempo.SLOWEST_BPM, Tempo.FASTEST_BPM);
        if (clamped != rounded) {
            report.warn("A tempo of " + rounded + " BPM was changed to " + clamped + " BPM, the closest that can be played");
        }
        return clamped;
    }

    private Fraction duration(XMLStreamReader xml) throws XMLStreamException {
        Fraction[] duration = {Fraction.ZERO};
        Xml.children(xml, child -> {
            if (child.equals("duration")) {
                duration[0] = Fraction.of(Integer.parseInt(Xml.text(xml)), divisions * 4L);
            } else {
                Xml.skip(xml);
            }
        });
        return duration[0];
    }

    private void advance(Fraction duration) {
        position = position.plus(duration);
        if (position.compareTo(furthest) > 0) {
            furthest = position;
        }
    }

    private TimeSignature timeSignatureAt(Fraction at) {
        Map.Entry<Fraction, TimeSignature> entry = timeSignatures.floorEntry(at);
        return entry == null ? TimeSignature.COMMON_TIME : entry.getValue();
    }

    private static Tie tieOf(Set<Tie> ties) {
        if (ties.contains(Tie.START) && ties.contains(Tie.STOP)) {
            return Tie.CONTINUE;
        }
        return ties.isEmpty() ? Tie.NONE : ties.iterator().next();
    }

    private static int parseVoice(String voice) {
        try {
            return Math.max(1, Integer.parseInt(voice));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static double parseLeadingNumber(String text) {
        try {
            return Double.parseDouble(text.replaceAll("[^0-9.].*$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static final class NoteElement {
        private boolean grace;
        private boolean cue;
        private boolean chord;
        private boolean rest;
        private boolean unpitched;
        private char step;
        private double alter;
        private int octave;
        private int duration;
        private int voice = 1;
        private int staff = 1;
        private final Set<Tie> ties = EnumSet.noneOf(Tie.class);
    }
}
