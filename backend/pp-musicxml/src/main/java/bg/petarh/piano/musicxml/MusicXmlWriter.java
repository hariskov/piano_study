package bg.petarh.piano.musicxml;

import bg.petarh.piano.model.key.KeyMode;
import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.score.Part;
import bg.petarh.piano.model.score.Placed;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.score.ScoreNote;
import bg.petarh.piano.model.score.Tie;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class MusicXmlWriter {

    private static final Fraction QUARTER = Fraction.of(1, 4);
    private static final Pitch MIDDLE_C = Pitch.parse("C4");

    private record Piece(ScoreNote note, Fraction start, Fraction duration, Tie tie) {
    }

    private record Lane(int staff, int voice) {
    }

    public MusicXmlWriter() {
    }

    public void write(Score score, OutputStream out) {
        try {
            out.write(toXml(score).getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the MusicXML score", e);
        }
    }

    String toXml(Score score) {
        long divisions = divisions(score);
        XmlBuilder xml = new XmlBuilder()
                .raw("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>")
                .raw("<!DOCTYPE score-partwise PUBLIC \"-//Recordare//DTD MusicXML 4.0 Partwise//EN\" "
                        + "\"http://www.musicxml.org/dtds/partwise.dtd\">")
                .open("score-partwise", "version", "4.0");
        xml.open("work").leaf("work-title", score.title()).close("work");
        score.composer().ifPresent(composer -> xml.open("identification")
                .leaf("creator", composer, "type", "composer").close("identification"));
        xml.open("part-list");
        for (Part part : score.parts()) {
            xml.open("score-part", "id", part.id()).leaf("part-name", part.name()).close("score-part");
        }
        xml.close("part-list");
        for (Part part : score.parts()) {
            writePart(xml, score, part, divisions);
        }
        return xml.close("score-partwise").toString();
    }

    private void writePart(XmlBuilder xml, Score score, Part part, long divisions) {
        xml.open("part", "id", part.id());
        List<Fraction> starts = score.measureStarts();
        boolean startsWithPickup = pickup(score, starts);
        for (int index = 0; index < starts.size(); index++) {
            Fraction start = starts.get(index);
            Fraction end = index + 1 < starts.size() ? starts.get(index + 1) : score.length();
            String number = String.valueOf(startsWithPickup ? index : index + 1);
            String[] attributes = startsWithPickup && index == 0
                    ? new String[]{"number", number, "implicit", "yes"}
                    : new String[]{"number", number};
            xml.open("measure", attributes);
            writeAttributes(xml, score, part, start, end, index == 0, divisions);
            writeTempos(xml, score, start, end, divisions);
            writeNotes(xml, part, start, end, divisions);
            xml.close("measure");
        }
        xml.close("part");
    }

    private static boolean pickup(Score score, List<Fraction> starts) {
        Fraction firstEnd = starts.size() > 1 ? starts.get(1) : score.length();
        return firstEnd.compareTo(score.timeSignatureAt(Fraction.ZERO).measureLength()) < 0;
    }

    private void writeAttributes(XmlBuilder xml, Score score, Part part, Fraction start, Fraction end, boolean first,
                                 long divisions) {
        KeySignature key = changeIn(score.keys(), start, end, first);
        TimeSignature time = changeIn(score.timeSignatures(), start, end, first);
        if (!first && key == null && time == null) {
            return;
        }
        xml.open("attributes");
        if (first) {
            xml.leaf("divisions", divisions);
        }
        if (key != null) {
            xml.open("key").leaf("fifths", key.fifths())
                    .leaf("mode", key.mode() == KeyMode.MINOR ? "minor" : "major").close("key");
        }
        if (time != null) {
            xml.open("time").leaf("beats", time.beats()).leaf("beat-type", time.beatUnit()).close("time");
        }
        if (first) {
            if (part.staffCount() == 2) {
                xml.leaf("staves", 2);
                clef(xml, "1", "G", 2);
                clef(xml, "2", "F", 4);
            } else if (mostlyLow(part)) {
                clef(xml, "1", "F", 4);
            } else {
                clef(xml, "1", "G", 2);
            }
        }
        xml.close("attributes");
    }

    private static void clef(XmlBuilder xml, String number, String sign, int line) {
        xml.open("clef", "number", number).leaf("sign", sign).leaf("line", line).close("clef");
    }

    private static boolean mostlyLow(Part part) {
        long low = part.notes().stream().filter(note -> note.pitch().compareTo(MIDDLE_C) < 0).count();
        return low * 2 > part.notes().size();
    }

    private static <T> T changeIn(List<Placed<T>> changes, Fraction start, Fraction end, boolean first) {
        T found = null;
        for (Placed<T> change : changes) {
            boolean inside = change.at().compareTo(start) >= 0 && change.at().compareTo(end) < 0;
            if (inside || (first && change.at().isZero())) {
                found = change.value();
            }
        }
        return found;
    }

    private void writeTempos(XmlBuilder xml, Score score, Fraction start, Fraction end, long divisions) {
        for (Placed<Tempo> change : score.tempos()) {
            if (change.at().compareTo(start) < 0 || change.at().compareTo(end) >= 0) {
                continue;
            }
            Tempo tempo = change.value();
            boolean quarterBeat = tempo.beatUnit().equals(QUARTER);
            xml.open("direction", "placement", "above").open("direction-type").open("metronome");
            NoteType beat = NoteType.of(tempo.beatUnit()).orElse(new NoteType("quarter", 0, false));
            xml.leaf("beat-unit", beat.name());
            if (beat.dots() > 0) {
                xml.empty("beat-unit-dot");
            }
            xml.leaf("per-minute", tempo.bpm()).close("metronome").close("direction-type");
            if (change.at().compareTo(start) > 0) {
                xml.leaf("offset", inDivisions(change.at().minus(start), divisions));
            }
            if (quarterBeat) {
                xml.empty("sound", "tempo", String.valueOf(tempo.bpm()));
            }
            xml.close("direction");
        }
    }

    private void writeNotes(XmlBuilder xml, Part part, Fraction start, Fraction end, long divisions) {
        Map<Lane, List<Piece>> lanes = new TreeMap<>(Comparator.comparingInt(Lane::staff).thenComparingInt(Lane::voice));
        for (ScoreNote note : part.notes()) {
            if (note.start().compareTo(end) < 0 && note.end().compareTo(start) > 0) {
                lanes.computeIfAbsent(new Lane(note.staff(), note.voice()), lane -> new ArrayList<>())
                        .add(piece(note, start, end));
            }
        }
        boolean twoStaves = part.staffCount() == 2;
        boolean firstLane = true;
        for (int staff = 1; staff <= part.staffCount(); staff++) {
            int current = staff;
            List<Map.Entry<Lane, List<Piece>>> onStaff = lanes.entrySet().stream()
                    .filter(lane -> lane.getKey().staff() == current).toList();
            if (onStaff.isEmpty() && (twoStaves || lanes.isEmpty())) {
                firstLane = backUp(xml, firstLane, start, end, divisions);
                measureRest(xml, staff, start, end, divisions, twoStaves);
            }
            for (Map.Entry<Lane, List<Piece>> lane : onStaff) {
                firstLane = backUp(xml, firstLane, start, end, divisions);
                writeLane(xml, lane.getKey(), lane.getValue(), start, end, divisions, twoStaves);
            }
        }
    }

    private static boolean backUp(XmlBuilder xml, boolean firstLane, Fraction start, Fraction end, long divisions) {
        if (!firstLane) {
            xml.open("backup").leaf("duration", inDivisions(end.minus(start), divisions)).close("backup");
        }
        return false;
    }

    // Voice 5 for the lower staff follows MuseScore, so a rest there never shares a voice with the upper staff.
    private static void measureRest(XmlBuilder xml, int staff, Fraction start, Fraction end, long divisions,
                                    boolean withStaff) {
        xml.open("note").empty("rest", "measure", "yes").leaf("duration", inDivisions(end.minus(start), divisions))
                .leaf("voice", staff == 1 ? 1 : 5);
        if (withStaff) {
            xml.leaf("staff", staff);
        }
        xml.close("note");
    }

    private void writeLane(XmlBuilder xml, Lane lane, List<Piece> pieces, Fraction start, Fraction end, long divisions,
                           boolean withStaff) {
        Map<Fraction, List<Piece>> chords = pieces.stream().collect(Collectors.groupingBy(Piece::start, TreeMap::new,
                Collectors.toList()));
        Fraction cursor = start;
        for (Map.Entry<Fraction, List<Piece>> chord : chords.entrySet()) {
            if (chord.getKey().compareTo(cursor) > 0) {
                rest(xml, lane, chord.getKey().minus(cursor), divisions, withStaff);
            }
            List<Piece> notes = chord.getValue().stream().sorted(Comparator.comparing(piece -> piece.note().pitch()))
                    .toList();
            for (int index = 0; index < notes.size(); index++) {
                note(xml, lane, notes.get(index), index > 0, divisions, withStaff);
            }
            cursor = chord.getKey().plus(notes.getFirst().duration());
        }
        if (end.compareTo(cursor) > 0) {
            rest(xml, lane, end.minus(cursor), divisions, withStaff);
        }
    }

    private void note(XmlBuilder xml, Lane lane, Piece piece, boolean chord, long divisions, boolean withStaff) {
        xml.open("note");
        if (chord) {
            xml.empty("chord");
        }
        xml.open("pitch").leaf("step", piece.note().spelling().step());
        if (piece.note().spelling().alter() != 0) {
            xml.leaf("alter", piece.note().spelling().alter());
        }
        xml.leaf("octave", piece.note().spelling().octave()).close("pitch");
        xml.leaf("duration", inDivisions(piece.duration(), divisions));
        boolean tieStop = piece.tie() == Tie.STOP || piece.tie() == Tie.CONTINUE;
        boolean tieStart = piece.tie() == Tie.START || piece.tie() == Tie.CONTINUE;
        if (tieStop) {
            xml.empty("tie", "type", "stop");
        }
        if (tieStart) {
            xml.empty("tie", "type", "start");
        }
        xml.leaf("voice", lane.voice());
        typeOf(xml, piece.duration());
        if (withStaff) {
            xml.leaf("staff", lane.staff());
        }
        if (tieStop || tieStart) {
            xml.open("notations");
            if (tieStop) {
                xml.empty("tied", "type", "stop");
            }
            if (tieStart) {
                xml.empty("tied", "type", "start");
            }
            xml.close("notations");
        }
        xml.close("note");
    }

    private void rest(XmlBuilder xml, Lane lane, Fraction duration, long divisions, boolean withStaff) {
        xml.open("note").empty("rest").leaf("duration", inDivisions(duration, divisions)).leaf("voice", lane.voice());
        typeOf(xml, duration);
        if (withStaff) {
            xml.leaf("staff", lane.staff());
        }
        xml.close("note");
    }

    private static void typeOf(XmlBuilder xml, Fraction duration) {
        NoteType.of(duration).ifPresent(type -> {
            xml.leaf("type", type.name());
            for (int dot = 0; dot < type.dots(); dot++) {
                xml.empty("dot");
            }
            if (type.triplet()) {
                xml.open("time-modification").leaf("actual-notes", 3).leaf("normal-notes", 2).close("time-modification");
            }
        });
    }

    private static Piece piece(ScoreNote note, Fraction start, Fraction end) {
        boolean startsBefore = note.start().compareTo(start) < 0;
        boolean endsAfter = note.end().compareTo(end) > 0;
        Fraction from = startsBefore ? start : note.start();
        Fraction to = endsAfter ? end : note.end();
        boolean stop = startsBefore || note.tie() == Tie.STOP || note.tie() == Tie.CONTINUE;
        boolean begin = endsAfter || note.tie() == Tie.START || note.tie() == Tie.CONTINUE;
        if (startsBefore && !endsAfter) {
            begin = note.tie() == Tie.START || note.tie() == Tie.CONTINUE;
        }
        if (endsAfter && !startsBefore) {
            stop = note.tie() == Tie.STOP || note.tie() == Tie.CONTINUE;
        }
        Tie tie = stop && begin ? Tie.CONTINUE : stop ? Tie.STOP : begin ? Tie.START : Tie.NONE;
        return new Piece(note, from, to.minus(from), tie);
    }

    private static long divisions(Score score) {
        List<Fraction> times = new ArrayList<>(score.measureStarts());
        times.add(score.length());
        score.tempos().forEach(change -> times.add(change.at()));
        for (Part part : score.parts()) {
            for (ScoreNote note : part.notes()) {
                times.add(note.start());
                times.add(note.duration());
            }
        }
        long divisions = 1;
        for (Fraction time : times.stream().filter(Objects::nonNull).toList()) {
            divisions = lcm(divisions, time.times(4).denominator());
        }
        return divisions;
    }

    private static long inDivisions(Fraction duration, long divisions) {
        Fraction value = duration.times(4 * divisions);
        return value.numerator();
    }

    private static long lcm(long a, long b) {
        return a / gcd(a, b) * b;
    }

    private static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }
}
