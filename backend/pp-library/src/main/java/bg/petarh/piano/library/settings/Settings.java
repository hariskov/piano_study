package bg.petarh.piano.library.settings;

import bg.petarh.piano.model.keyboard.KeyboardRange;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Settings {

    static final int LARGEST_LATENCY_OFFSET_MS = 500;

    private static final Settings DEFAULTS = new Settings(null, null, Map.of(), Map.of(), 0);

    private final String input;
    private final String output;
    private final Map<String, KeyboardRange> keyboards;
    private final Map<String, Integer> tempos;
    private final int latencyOffsetMs;

    private Settings(String input, String output, Map<String, KeyboardRange> keyboards, Map<String, Integer> tempos,
                     int latencyOffsetMs) {
        this.input = input;
        this.output = output;
        this.keyboards = Map.copyOf(keyboards);
        this.tempos = Map.copyOf(tempos);
        this.latencyOffsetMs = latencyOffsetMs;
    }

    public static Settings defaults() {
        return DEFAULTS;
    }

    public Optional<String> input() {
        return Optional.ofNullable(input);
    }

    public Optional<String> output() {
        return Optional.ofNullable(output);
    }

    public Optional<KeyboardRange> keyboardFor(String deviceId) {
        return Optional.ofNullable(keyboards.get(deviceId));
    }

    public Map<String, KeyboardRange> keyboards() {
        return keyboards;
    }

    public Optional<Integer> tempoFor(String mode) {
        return Optional.ofNullable(tempos.get(mode));
    }

    public Map<String, Integer> tempos() {
        return tempos;
    }

    public int latencyOffsetMs() {
        return latencyOffsetMs;
    }

    public Settings withInput(String deviceId) {
        return new Settings(Objects.requireNonNull(deviceId), output, keyboards, tempos, latencyOffsetMs);
    }

    public Settings withOutput(String deviceId) {
        return new Settings(input, Objects.requireNonNull(deviceId), keyboards, tempos, latencyOffsetMs);
    }

    public Settings withKeyboard(String deviceId, KeyboardRange range) {
        Map<String, KeyboardRange> changed = new HashMap<>(keyboards);
        changed.put(deviceId, range);
        return new Settings(input, output, changed, tempos, latencyOffsetMs);
    }

    public Settings withTempo(String mode, int bpm) {
        Map<String, Integer> changed = new HashMap<>(tempos);
        changed.put(mode, bpm);
        return new Settings(input, output, keyboards, changed, latencyOffsetMs);
    }

    public Settings withLatencyOffset(int milliseconds) {
        if (Math.abs(milliseconds) > LARGEST_LATENCY_OFFSET_MS) {
            throw new IllegalArgumentException("Latency offset must be within ±" + LARGEST_LATENCY_OFFSET_MS
                    + " ms: " + milliseconds);
        }
        return new Settings(input, output, keyboards, tempos, milliseconds);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Settings settings
                && Objects.equals(settings.input, input)
                && Objects.equals(settings.output, output)
                && settings.keyboards.equals(keyboards)
                && settings.tempos.equals(tempos)
                && settings.latencyOffsetMs == latencyOffsetMs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, output, keyboards, tempos, latencyOffsetMs);
    }

    @Override
    public String toString() {
        return "Settings[input=" + input + ", output=" + output + ", keyboards=" + keyboards + ", tempos=" + tempos
                + ", latencyOffsetMs=" + latencyOffsetMs + "]";
    }
}
