package bg.petarh.piano.app.cli;

import bg.petarh.piano.model.chord.ChordType;
import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardPreset;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.pitch.Pitch;
import bg.petarh.piano.model.time.TimeSignature;
import picocli.CommandLine.ITypeConverter;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PracticeConverters {

    private static final Pattern RANGE = Pattern.compile("([A-Ga-g][#b]?-?\\d)-([A-Ga-g][#b]?-?\\d)");
    private static final Pattern KEY = Pattern.compile("([A-Ga-g][#b]?)(m?)");

    private PracticeConverters() {
    }

    static final class KeyConverter implements ITypeConverter<KeySignature> {

        @Override
        public KeySignature convert(String value) {
            Matcher matcher = KEY.matcher(value);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Expected a key like C, Bb, F#m or am: " + value);
            }
            return matcher.group(2).isEmpty()
                    ? KeySignature.major(matcher.group(1))
                    : KeySignature.minor(matcher.group(1));
        }
    }

    static final class RangeConverter implements ITypeConverter<KeyboardRange> {

        @Override
        public KeyboardRange convert(String value) {
            Matcher matcher = RANGE.matcher(value);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Expected a range like C4-C6: " + value);
            }
            return KeyboardRange.of(Pitch.parse(matcher.group(1)), Pitch.parse(matcher.group(2)));
        }
    }

    static final class KeyboardConverter implements ITypeConverter<KeyboardRange> {

        @Override
        public KeyboardRange convert(String value) {
            if (!value.chars().allMatch(Character::isDigit) || value.isEmpty()) {
                return new RangeConverter().convert(value);
            }
            int keys = Integer.parseInt(value);
            return Arrays.stream(KeyboardPreset.values())
                    .filter(preset -> preset.keyCount() == keys)
                    .findFirst()
                    .map(KeyboardPreset::range)
                    .orElseThrow(() -> new IllegalArgumentException("No " + keys + "-key preset; give a range like F1-F7"));
        }
    }

    static final class TimeSignatureConverter implements ITypeConverter<TimeSignature> {

        @Override
        public TimeSignature convert(String value) {
            return TimeSignature.parse(value);
        }
    }

    static final class ChordTypeConverter implements ITypeConverter<ChordType> {

        @Override
        public ChordType convert(String value) {
            return ChordType.forCode(value);
        }
    }
}
