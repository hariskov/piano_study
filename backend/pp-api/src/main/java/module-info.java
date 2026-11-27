module bg.petarh.piano.api {
    requires tools.jackson.databind;

    exports bg.petarh.piano.api.rpc;
    exports bg.petarh.piano.api.stdio;

    opens bg.petarh.piano.api.rpc to tools.jackson.databind;
}
