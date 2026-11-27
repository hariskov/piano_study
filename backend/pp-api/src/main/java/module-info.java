module bg.petarh.piano.api {
    requires transitive bg.petarh.piano.midi;
    requires tools.jackson.databind;

    exports bg.petarh.piano.api;
    exports bg.petarh.piano.api.rpc;
    exports bg.petarh.piano.api.stdio;

    opens bg.petarh.piano.api.rpc to tools.jackson.databind;
    opens bg.petarh.piano.api.devices to tools.jackson.databind;
}
