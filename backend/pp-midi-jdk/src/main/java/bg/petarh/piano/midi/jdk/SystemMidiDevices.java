package bg.petarh.piano.midi.jdk;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import java.util.ArrayList;
import java.util.List;

final class SystemMidiDevices implements MidiDeviceSource {

    @Override
    public List<MidiDevice> devices() {
        List<MidiDevice> devices = new ArrayList<>();
        for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (!(device instanceof Sequencer)) {
                    devices.add(device);
                }
            } catch (MidiUnavailableException ignored) {
                // A device unplugged between listing and lookup is simply left out of this scan.
            }
        }
        return devices;
    }
}
