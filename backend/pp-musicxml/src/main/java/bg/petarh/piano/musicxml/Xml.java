package bg.petarh.piano.musicxml;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;

final class Xml {

    @FunctionalInterface
    interface ChildHandler {
        void handle(String name) throws XMLStreamException;
    }

    private Xml() {
    }

    static XMLStreamReader open(InputStream in) throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        // Scores come from anywhere, so the parser must never fetch a DTD or resolve an external entity.
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        return factory.createXMLStreamReader(in);
    }

    static void toRoot(XMLStreamReader xml) throws XMLStreamException {
        while (xml.next() != XMLStreamConstants.START_ELEMENT) {
            if (!xml.hasNext()) {
                throw new XMLStreamException("The file has no elements");
            }
        }
    }

    static void children(XMLStreamReader xml, ChildHandler handler) throws XMLStreamException {
        while (xml.hasNext()) {
            int event = xml.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                handler.handle(xml.getLocalName());
                if (xml.getEventType() == XMLStreamConstants.START_ELEMENT) {
                    skip(xml);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    static String text(XMLStreamReader xml) throws XMLStreamException {
        return xml.getElementText().trim();
    }

    static void skip(XMLStreamReader xml) throws XMLStreamException {
        int depth = 1;
        while (depth > 0 && xml.hasNext()) {
            int event = xml.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                depth++;
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                depth--;
            }
        }
    }
}
