package io.github.hariskov.piano.midi.device;

public final class DeviceId {

    private final String value;

    private DeviceId(String value) {
        this.value = value;
    }

    public static DeviceId of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Device id must not be blank");
        }
        return new DeviceId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof DeviceId id && id.value.equals(value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
