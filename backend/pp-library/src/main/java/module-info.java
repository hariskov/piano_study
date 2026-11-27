module bg.petarh.piano.library {
    requires transitive bg.petarh.piano.model;
    requires tools.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    exports bg.petarh.piano.library.settings;

    opens bg.petarh.piano.library.settings to tools.jackson.databind;
}
