package bg.petarh.piano.api.rpc;

public record ApiEvent<P>(String name, Class<P> paramsType) {

    public static <P> ApiEvent<P> of(String name, Class<P> paramsType) {
        return new ApiEvent<>(name, paramsType);
    }

    public void send(EventSink sink, P params) {
        sink.emit(name, params);
    }
}
