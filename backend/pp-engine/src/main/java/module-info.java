module bg.petarh.piano.engine {
    requires transitive bg.petarh.piano.midi;

    exports bg.petarh.piano.engine.keyboard;
    exports bg.petarh.piano.engine.time;
    exports bg.petarh.piano.engine.targets;
    exports bg.petarh.piano.engine.session;
    exports bg.petarh.piano.engine.evaluation;
    exports bg.petarh.piano.engine.run;
}
