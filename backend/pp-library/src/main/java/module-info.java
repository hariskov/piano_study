module bg.petarh.piano.library {
    requires transitive bg.petarh.piano.model;
    requires transitive bg.petarh.piano.musicxml;
    requires tools.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    exports bg.petarh.piano.library.settings;
    exports bg.petarh.piano.library.songs;

    opens bg.petarh.piano.library.settings to tools.jackson.databind;
    opens bg.petarh.piano.library.songs to tools.jackson.databind;
}
