package io.github.hariskov.piano.midi;

public record DeviceId(String value) {

    public DeviceId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Device id must not be blank");
        }
    }
}
