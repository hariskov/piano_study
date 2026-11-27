package bg.petarh.piano.api.rpc;

public record MethodSignature(String name, Class<?> paramsType, Class<?> resultType) {
}
