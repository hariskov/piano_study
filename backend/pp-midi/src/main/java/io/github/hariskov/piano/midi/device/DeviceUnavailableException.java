package io.github.hariskov.piano.midi.device;

import java.io.Serial;

public class DeviceUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DeviceUnavailableException(DeviceId id) {
        super("MIDI device unavailable: " + id);
    }

    public DeviceUnavailableException(DeviceId id, Throwable cause) {
        super("MIDI device unavailable: " + id, cause);
    }
}
