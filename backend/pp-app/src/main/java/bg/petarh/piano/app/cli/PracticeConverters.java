package bg.petarh.piano.app.cli;

import bg.petarh.piano.model.chord.ChordType;
import bg.petarh.piano.model.key.KeySignature;
import bg.petarh.piano.model.keyboard.KeyboardRange;
import bg.petarh.piano.model.time.TimeSignature;
import picocli.CommandLine.ITypeConverter;

final class PracticeConverters {

    private PracticeConverters() {
    }

    static final class KeyConverter implements ITypeConverter<KeySignature> {

        @Override
        public KeySignature convert(String value) {
            return KeySignature.parse(value);
        }
    }

    static final class RangeConverter implements ITypeConverter<KeyboardRange> {

        @Override
        public KeyboardRange convert(String value) {
            return KeyboardRange.parse(value);
        }
    }

    static final class KeyboardConverter implements ITypeConverter<KeyboardRange> {

        @Override
        public KeyboardRange convert(String value) {
            return KeyboardRange.parseKeyboard(value);
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
