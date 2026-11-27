package io.github.hariskov.piano.midi;

import java.io.Serial;

public class DeviceUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DeviceUnavailableException(DeviceId id) {
        super("MIDI device unavailable: " + id.value());
    }

    public DeviceUnavailableException(DeviceId id, Throwable cause) {
        super("MIDI device unavailable: " + id.value(), cause);
    }
}
