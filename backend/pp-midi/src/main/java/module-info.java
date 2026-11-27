module io.github.hariskov.piano.midi {
    requires transitive io.github.hariskov.piano.model;

    exports io.github.hariskov.piano.midi.device;
    exports io.github.hariskov.piano.midi.value;
    exports io.github.hariskov.piano.midi.input;
    exports io.github.hariskov.piano.midi.output;
    exports io.github.hariskov.piano.midi.virtual.input;
    exports io.github.hariskov.piano.midi.virtual.output;
}
