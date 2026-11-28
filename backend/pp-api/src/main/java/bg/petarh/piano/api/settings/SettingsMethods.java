package bg.petarh.piano.api.settings;

import bg.petarh.piano.api.rpc.MayBeAbsent;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.engine.evaluation.FollowMode;
import bg.petarh.piano.library.settings.Settings;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.time.Tempo;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class SettingsMethods {

    public record SettingsView(@MayBeAbsent String input, @MayBeAbsent String output, Map<String, String> keyboards, Map<String, Integer> tempos,
                        int latencyOffsetMs, int instrument, boolean keySound) {

        static SettingsView of(Settings settings) {
            Map<String, String> keyboards = new TreeMap<>();
            settings.keyboards().forEach((device, range) -> keyboards.put(device, range.toString()));
            return new SettingsView(settings.input().orElse(null), settings.output().orElse(null), keyboards,
                    new TreeMap<>(settings.tempos()), settings.latencyOffsetMs(), settings.instrument(), settings.keySound());
        }
    }

    public record SettingsUpdate(String input, String output, Map<String, String> keyboards, Map<String, Integer> tempos,
                          Integer latencyOffsetMs, Integer instrument, Boolean keySound) {

        Settings applyTo(Settings settings) {
            Settings changed = settings;
            if (input != null) {
                changed = changed.withInput(input);
            }
            if (output != null) {
                changed = changed.withOutput(output);
            }
            for (Map.Entry<String, String> keyboard : orEmpty(keyboards).entrySet()) {
                changed = changed.withKeyboard(keyboard.getKey(), KeyboardRange.parseKeyboard(keyboard.getValue()));
            }
            for (Map.Entry<String, Integer> tempo : orEmpty(tempos).entrySet()) {
                FollowMode.valueOf(tempo.getKey().toUpperCase(Locale.ROOT));
                changed = changed.withTempo(tempo.getKey(), Tempo.of(tempo.getValue()).bpm());
            }
            if (latencyOffsetMs != null) {
                changed = changed.withLatencyOffset(latencyOffsetMs);
            }
            if (instrument != null) {
                changed = changed.withInstrument(instrument);
            }
            if (keySound != null) {
                changed = changed.withKeySound(keySound);
            }
            return changed;
        }

        private static <V> Map<String, V> orEmpty(Map<String, V> values) {
            return values == null ? Map.of() : values;
        }
    }

    private SettingsMethods() {
    }

    public static void register(RpcServer server, SettingsService settings) {
        server.register("settings.get", Void.class, SettingsView.class, ignored -> SettingsView.of(settings.current()));
        server.register("settings.update", SettingsUpdate.class, SettingsView.class,
                update -> SettingsView.of(settings.update(update::applyTo)));
    }
}
