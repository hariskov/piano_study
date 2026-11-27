package bg.petarh.piano.api.rpc;

@FunctionalInterface
public interface EventSink {

    void emit(String method, Object params);
}
