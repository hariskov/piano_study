package bg.petarh.piano.library.settings;

import bg.petarh.piano.model.keyboard.KeyboardRange;

import java.util.Map;
import java.util.TreeMap;

record SettingsFile(String input, String output, Map<String, String> keyboards, Map<String, Integer> tempos,
                    Integer latencyOffsetMs, Integer instrument, Boolean keySound) {

    static SettingsFile of(Settings settings) {
        Map<String, String> keyboards = new TreeMap<>();
        settings.keyboards().forEach((device, range) -> keyboards.put(device, range.toString()));
        return new SettingsFile(settings.input().orElse(null), settings.output().orElse(null), keyboards,
                new TreeMap<>(settings.tempos()), settings.latencyOffsetMs(), settings.instrument(), settings.keySound());
    }

    Settings toSettings() {
        Settings settings = Settings.defaults();
        if (input != null) {
            settings = settings.withInput(input);
        }
        if (output != null) {
            settings = settings.withOutput(output);
        }
        for (Map.Entry<String, String> keyboard : orEmpty(keyboards).entrySet()) {
            try {
                settings = settings.withKeyboard(keyboard.getKey(), KeyboardRange.parse(keyboard.getValue()));
            } catch (IllegalArgumentException e) {
                // A range that can't be read is forgotten; the device then falls back to the default range.
            }
        }
        for (Map.Entry<String, Integer> tempo : orEmpty(tempos).entrySet()) {
            settings = settings.withTempo(tempo.getKey(), tempo.getValue());
        }
        if (latencyOffsetMs != null && Math.abs(latencyOffsetMs) <= Settings.LARGEST_LATENCY_OFFSET_MS) {
            settings = settings.withLatencyOffset(latencyOffsetMs);
        }
        if (instrument != null && instrument >= 0 && instrument <= Settings.LAST_INSTRUMENT) {
            settings = settings.withInstrument(instrument);
        }
        if (keySound != null) {
            settings = settings.withKeySound(keySound);
        }
        return settings;
    }

    private static <V> Map<String, V> orEmpty(Map<String, V> values) {
        return values == null ? Map.of() : values;
    }
}
