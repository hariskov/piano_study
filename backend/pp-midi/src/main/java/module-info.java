module bg.petarh.piano.midi {
    requires transitive bg.petarh.piano.model;

    exports bg.petarh.piano.midi.device;
    exports bg.petarh.piano.midi.value;
    exports bg.petarh.piano.midi.input;
    exports bg.petarh.piano.midi.output;
    exports bg.petarh.piano.midi.virtual.input;
    exports bg.petarh.piano.midi.virtual.output;
}
