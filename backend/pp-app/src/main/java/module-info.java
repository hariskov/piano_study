module bg.petarh.piano.app {
    requires bg.petarh.piano.midi.jdk;
    requires bg.petarh.piano.engine;
    requires info.picocli;

    opens bg.petarh.piano.app.cli to info.picocli;
}
