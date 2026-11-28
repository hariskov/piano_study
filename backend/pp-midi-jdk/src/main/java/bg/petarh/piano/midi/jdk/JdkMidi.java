package bg.petarh.piano.midi.jdk;

import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.input.TimestampSource;
import bg.petarh.piano.midi.output.MidiOutputPort;

import java.nio.file.Path;

public final class JdkMidi {

    private JdkMidi() {
    }

    public static MidiInputPort inputPort() {
        return inputPort(TimestampSource.system());
    }

    public static MidiInputPort inputPort(TimestampSource clock) {
        return new JdkMidiInputPort(systemCatalog(), clock);
    }

    public static MidiOutputPort outputPort() {
        return new JdkMidiOutputPort(systemCatalog(), new SoundFontLoader(
                () -> SoundFonts.find(System::getenv, Path.of(System.getProperty("user.home")), SoundFonts.SYSTEM_FOLDERS)));
    }

    private static JdkDeviceCatalog systemCatalog() {
        return new JdkDeviceCatalog(new SystemMidiDevices());
    }
}
