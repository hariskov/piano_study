package bg.petarh.piano.app.lifetime;

@FunctionalInterface
public interface Lifetime {

    void keepOpenUntilStopped(AutoCloseable resource) throws Exception;
}
