package bg.petarh.piano.musicxml;

final class XmlBuilder {

    private final StringBuilder out = new StringBuilder();
    private int depth;

    XmlBuilder raw(String line) {
        out.append(line).append('\n');
        return this;
    }

    XmlBuilder open(String element, String... attributes) {
        indent().append('<').append(element).append(attributesOf(attributes)).append(">\n");
        depth++;
        return this;
    }

    XmlBuilder close(String element) {
        depth--;
        indent().append("</").append(element).append(">\n");
        return this;
    }

    XmlBuilder leaf(String element, Object text, String... attributes) {
        indent().append('<').append(element).append(attributesOf(attributes)).append('>').append(escape(String.valueOf(text)))
                .append("</").append(element).append(">\n");
        return this;
    }

    XmlBuilder empty(String element, String... attributes) {
        indent().append('<').append(element).append(attributesOf(attributes)).append("/>\n");
        return this;
    }

    @Override
    public String toString() {
        return out.toString();
    }

    private StringBuilder indent() {
        return out.append("  ".repeat(depth));
    }

    private static String attributesOf(String... attributes) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i + 1 < attributes.length; i += 2) {
            text.append(' ').append(attributes[i]).append("=\"").append(escape(attributes[i + 1])).append('"');
        }
        return text.toString();
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
