package io.github.hariskov.piano.midi.jdk;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.device.DeviceId;
import io.github.hariskov.piano.midi.device.DeviceUnavailableException;
import io.github.hariskov.piano.midi.jdk.message.OutputMessageEncoder;
import io.github.hariskov.piano.midi.output.MidiOutputPort;
import io.github.hariskov.piano.midi.output.OutputConnection;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import java.util.List;

final class JdkMidiOutputPort implements MidiOutputPort {

    private final JdkDeviceCatalog catalog;
    private final OutputMessageEncoder encoder = new OutputMessageEncoder();

    JdkMidiOutputPort(JdkDeviceCatalog catalog) {
        this.catalog = catalog;
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
            return new JdkOutputConnection(entry.descriptor(), device, device.getReceiver(), encoder);
        } catch (MidiUnavailableException e) {
            device.close();
            throw new DeviceUnavailableException(id, e);
        } catch (RuntimeException e) {
            device.close();
            throw e;
        }
    }
}
