module bg.petarh.piano.api {
    requires transitive bg.petarh.piano.midi;
    requires bg.petarh.piano.engine;
    requires bg.petarh.piano.generator;
    requires tools.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    exports bg.petarh.piano.api;
    exports bg.petarh.piano.api.rpc;
    exports bg.petarh.piano.api.stdio;

    opens bg.petarh.piano.api.rpc to tools.jackson.databind;
    opens bg.petarh.piano.api.devices to tools.jackson.databind;
    opens bg.petarh.piano.api.session to tools.jackson.databind;
}
