package bg.petarh.piano.api.rpc;

@FunctionalInterface
public interface RpcMethod<P> {

    Object call(P params);
}
