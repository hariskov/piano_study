module bg.petarh.piano.app {
    requires bg.petarh.piano.midi.jdk;
    requires bg.petarh.piano.engine;
    requires bg.petarh.piano.generator;
    requires bg.petarh.piano.api;
    requires bg.petarh.piano.library;
    requires info.picocli;

    opens bg.petarh.piano.app.cli to info.picocli;
}
