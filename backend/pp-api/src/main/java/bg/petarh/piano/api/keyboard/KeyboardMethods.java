package bg.petarh.piano.api.keyboard;

import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.model.pitch.Pitch;

public final class KeyboardMethods {

    private static final int DEFAULT_VELOCITY = 80;

    public record KeyParams(String pitch, Integer velocity) {

        Pitch parsedPitch() {
            if (pitch == null) {
                throw new IllegalArgumentException("pitch is required, e.g. C4");
            }
            return Pitch.parse(pitch);
        }
    }

    private KeyboardMethods() {
    }

    public static void register(RpcServer server, VirtualKeyboard keyboard) {
        server.register("keyboard.press", KeyParams.class, Void.class, key -> {
            keyboard.press(key.parsedPitch(), Velocity.of(key.velocity() == null ? DEFAULT_VELOCITY : key.velocity()));
            return null;
        });
        server.register("keyboard.release", KeyParams.class, Void.class, key -> {
            keyboard.release(key.parsedPitch());
            return null;
        });
    }
}
