package bg.petarh.piano.midi.jdk;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.jdk.message.OutputMessageEncoder;
import bg.petarh.piano.midi.output.MidiOutputPort;
import bg.petarh.piano.midi.output.OutputConnection;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

final class JdkMidiOutputPort implements MidiOutputPort {

    private final JdkDeviceCatalog catalog;
    private final Supplier<Optional<Soundbank>> soundFont;
    private final OutputMessageEncoder encoder = new OutputMessageEncoder();

    JdkMidiOutputPort(JdkDeviceCatalog catalog) {
        this(catalog, Optional::empty);
    }

    JdkMidiOutputPort(JdkDeviceCatalog catalog, Supplier<Optional<Soundbank>> soundFont) {
        this.catalog = catalog;
        this.soundFont = soundFont;
    }

    @Override
    public List<DeviceDescriptor> devices() {
        return catalog.outputs();
    }

    @Override
    public OutputConnection open(DeviceId id) {
        JdkDeviceCatalog.Entry entry = catalog.findOutput(id).orElseThrow(() -> new DeviceUnavailableException(id));
        MidiDevice device = entry.device();
        try {
            device.open();
            if (device instanceof Synthesizer synthesizer) {
                soundFont.get().ifPresent(bank -> play(synthesizer, bank));
            }
            return new JdkOutputConnection(entry.descriptor(), device, device.getReceiver(), encoder);
        } catch (MidiUnavailableException e) {
            device.close();
            throw new DeviceUnavailableException(id, e);
        } catch (RuntimeException e) {
            device.close();
            throw e;
        }
    }

    private static void play(Synthesizer synthesizer, Soundbank bank) {
        if (!synthesizer.isSoundbankSupported(bank)) {
            return;
        }
        Soundbank builtIn = synthesizer.getDefaultSoundbank();
        if (builtIn != null) {
            synthesizer.unloadAllInstruments(builtIn);
        }
        synthesizer.loadAllInstruments(bank);
    }
}
