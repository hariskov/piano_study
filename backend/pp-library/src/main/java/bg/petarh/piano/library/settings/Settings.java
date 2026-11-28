package bg.petarh.piano.library.settings;

import bg.petarh.piano.model.keyboard.KeyboardRange;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Settings {

    static final int LARGEST_LATENCY_OFFSET_MS = 500;
    static final int LAST_INSTRUMENT = 127;

    private static final Settings DEFAULTS = new Settings(null, null, Map.of(), Map.of(), 0, 0, false);

    private final String input;
    private final String output;
    private final Map<String, KeyboardRange> keyboards;
    private final Map<String, Integer> tempos;
    private final int latencyOffsetMs;
    private final int instrument;
    private final boolean keySound;

    private Settings(String input, String output, Map<String, KeyboardRange> keyboards, Map<String, Integer> tempos,
                     int latencyOffsetMs, int instrument, boolean keySound) {
        this.input = input;
        this.output = output;
        this.keyboards = Map.copyOf(keyboards);
        this.tempos = Map.copyOf(tempos);
        this.latencyOffsetMs = latencyOffsetMs;
        this.instrument = instrument;
        this.keySound = keySound;
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

    /** The General MIDI program (0–127) that the keys you play sound with. */
    public int instrument() {
        return instrument;
    }

    /** Whether the keys you play are also sent to the output. */
    public boolean keySound() {
        return keySound;
    }

    public Settings withInput(String deviceId) {
        return new Settings(Objects.requireNonNull(deviceId), output, keyboards, tempos, latencyOffsetMs, instrument,
                keySound);
    }

    public Settings withOutput(String deviceId) {
        return new Settings(input, Objects.requireNonNull(deviceId), keyboards, tempos, latencyOffsetMs, instrument,
                keySound);
    }

    public Settings withKeyboard(String deviceId, KeyboardRange range) {
        Map<String, KeyboardRange> changed = new HashMap<>(keyboards);
        changed.put(deviceId, range);
        return new Settings(input, output, changed, tempos, latencyOffsetMs, instrument, keySound);
    }

    public Settings withTempo(String mode, int bpm) {
        Map<String, Integer> changed = new HashMap<>(tempos);
        changed.put(mode, bpm);
        return new Settings(input, output, keyboards, changed, latencyOffsetMs, instrument, keySound);
    }

    public Settings withLatencyOffset(int milliseconds) {
        if (Math.abs(milliseconds) > LARGEST_LATENCY_OFFSET_MS) {
            throw new IllegalArgumentException("Latency offset must be within ±" + LARGEST_LATENCY_OFFSET_MS
                    + " ms: " + milliseconds);
        }
        return new Settings(input, output, keyboards, tempos, milliseconds, instrument, keySound);
    }

    public Settings withInstrument(int program) {
        if (program < 0 || program > LAST_INSTRUMENT) {
            throw new IllegalArgumentException("Instrument must be a General MIDI program 0-127: " + program);
        }
        return new Settings(input, output, keyboards, tempos, latencyOffsetMs, program, keySound);
    }

    public Settings withKeySound(boolean on) {
        return new Settings(input, output, keyboards, tempos, latencyOffsetMs, instrument, on);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Settings settings
                && Objects.equals(settings.input, input)
                && Objects.equals(settings.output, output)
                && settings.keyboards.equals(keyboards)
                && settings.tempos.equals(tempos)
                && settings.latencyOffsetMs == latencyOffsetMs
                && settings.instrument == instrument
                && settings.keySound == keySound;
    }

    @Override
    public int hashCode() {
        return Objects.hash(input, output, keyboards, tempos, latencyOffsetMs, instrument, keySound);
    }

    @Override
    public String toString() {
        return "Settings[input=" + input + ", output=" + output + ", keyboards=" + keyboards + ", tempos=" + tempos
                + ", latencyOffsetMs=" + latencyOffsetMs + ", instrument=" + instrument + ", keySound=" + keySound + "]";
    }
}
