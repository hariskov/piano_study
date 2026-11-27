package bg.petarh.piano.musicxml;

import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.score.Score;
import bg.petarh.piano.model.time.Fraction;
import bg.petarh.piano.model.time.Tempo;
import bg.petarh.piano.model.time.TimeSignature;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class MusicXmlReader {

    private static final String CONTAINER = "META-INF/container.xml";

    public MusicXmlReader() {
    }

    public ImportResult read(Path file) throws IOException {
        String fileName = file.getFileName().toString();
        if (fileName.toLowerCase(Locale.ROOT).endsWith(".mxl")) {
            return readCompressed(file, fileName);
        }
        try (InputStream in = Files.newInputStream(file)) {
            return read(in, fileName);
        }
    }

    public ImportResult read(InputStream xmlInput, String fileName) {
        try {
            XMLStreamReader xml = Xml.open(xmlInput);
            Xml.toRoot(xml);
            String root = xml.getLocalName();
            if (root.equals("score-timewise")) {
                throw new MusicXmlException(fileName + ": score-timewise MusicXML is not supported; save it as partwise");
            }
            if (!root.equals("score-partwise")) {
                throw new MusicXmlException(fileName + ": not a MusicXML score (the root element is <" + root + ">)");
            }
            return new ScoreReader(fileName).read(xml);
        } catch (XMLStreamException | RuntimeException e) {
            if (e instanceof MusicXmlException musicXml) {
                throw musicXml;
            }
            throw new MusicXmlException(fileName + ": not a readable MusicXML file (" + e.getMessage() + ")", e);
        }
    }

    private ImportResult readCompressed(Path file, String fileName) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry score = rootFile(zip, fileName);
            try (InputStream in = zip.getInputStream(score)) {
                return read(in, fileName);
            }
        }
    }

    private static ZipEntry rootFile(ZipFile zip, String fileName) throws IOException {
        ZipEntry container = zip.getEntry(CONTAINER);
        if (container != null) {
            try (InputStream in = zip.getInputStream(container)) {
                String path = rootFilePath(in);
                if (path != null && zip.getEntry(path) != null) {
                    return zip.getEntry(path);
                }
            } catch (XMLStreamException e) {
                throw new MusicXmlException(fileName + ": its " + CONTAINER + " can't be read", e);
            }
        }
        return zip.stream()
                .filter(entry -> !entry.getName().startsWith("META-INF/"))
                .filter(entry -> entry.getName().endsWith(".xml") || entry.getName().endsWith(".musicxml"))
                .findFirst()
                .orElseThrow(() -> new MusicXmlException(fileName + ": the archive holds no MusicXML score"));
    }

    private static String rootFilePath(InputStream container) throws XMLStreamException {
        XMLStreamReader xml = Xml.open(container);
        while (xml.hasNext()) {
            if (xml.next() == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals("rootfile")) {
                return xml.getAttributeValue(null, "full-path");
            }
        }
        return null;
    }

    private static final class ScoreReader {

        private final String fileName;
        private final ImportReport report = new ImportReport();
        private final Map<String, String> partNames = new LinkedHashMap<>();
        private final List<PartReader> parts = new ArrayList<>();
        private final TreeMap<Fraction, TimeSignature> timeSignatures = new TreeMap<>();
        private final TreeMap<Fraction, KeySignature> keys = new TreeMap<>();
        private final TreeMap<Fraction, Tempo> tempos = new TreeMap<>();
        private String workTitle;
        private String movementTitle;
        private String composer;

        private ScoreReader(String fileName) {
            this.fileName = fileName;
        }

        private ImportResult read(XMLStreamReader xml) throws XMLStreamException {
            Xml.children(xml, child -> {
                switch (child) {
                    case "work" -> Xml.children(xml, work -> {
                        if (work.equals("work-title")) {
                            workTitle = Xml.text(xml);
                        }
                    });
                    case "movement-title" -> movementTitle = Xml.text(xml);
                    case "identification" -> Xml.children(xml, identification -> {
                        if (identification.equals("creator") && "composer".equals(xml.getAttributeValue(null, "type"))) {
                            composer = Xml.text(xml);
                        }
                    });
                    case "part-list" -> partList(xml);
                    case "part" -> part(xml);
                    default -> Xml.skip(xml);
                }
            });
            return new ImportResult(score(), report.warnings());
        }

        private void partList(XMLStreamReader xml) throws XMLStreamException {
            Xml.children(xml, child -> {
                if (child.equals("score-part")) {
                    String id = xml.getAttributeValue(null, "id");
                    partNames.put(id, id);
                    Xml.children(xml, detail -> {
                        if (detail.equals("part-name")) {
                            partNames.put(id, Xml.text(xml));
                        }
                    });
                }
            });
        }

        private void part(XMLStreamReader xml) throws XMLStreamException {
            String id = xml.getAttributeValue(null, "id");
            PartReader part = new PartReader(id, partNames.getOrDefault(id, id), report, timeSignatures, keys, tempos);
            part.read(xml);
            parts.add(part);
        }

        private Score score() {
            Score.Builder builder = Score.builder(title()).composer(blankToNull(composer));
            Fraction length = parts.stream().map(PartReader::end).max(Fraction::compareTo).orElse(Fraction.ZERO);
            parts.forEach(part -> builder.part(part.part()));
            timeSignatures.forEach((at, signature) -> keepInside(at, length, () -> builder.timeSignature(at, signature)));
            keys.forEach((at, key) -> keepInside(at, length, () -> builder.key(at, key)));
            tempos.forEach((at, tempo) -> keepInside(at, length, () -> builder.tempo(at, tempo)));
            if (!parts.isEmpty()) {
                builder.measureStarts(parts.getFirst().measureStarts());
            }
            return builder.length(length).build();
        }

        private static void keepInside(Fraction at, Fraction length, Runnable add) {
            if (at.isZero() || at.compareTo(length) < 0) {
                add.run();
            }
        }

        private String title() {
            if (workTitle != null && !workTitle.isBlank()) {
                return workTitle;
            }
            if (movementTitle != null && !movementTitle.isBlank()) {
                return movementTitle;
            }
            int dot = fileName.lastIndexOf('.');
            return dot > 0 ? fileName.substring(0, dot) : fileName;
        }

        private static String blankToNull(String text) {
            return text == null || text.isBlank() ? null : text;
        }
    }
}
