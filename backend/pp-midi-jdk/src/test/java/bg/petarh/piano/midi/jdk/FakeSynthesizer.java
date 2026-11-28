package bg.petarh.piano.midi.jdk;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;
import java.util.ArrayList;
import java.util.List;

final class FakeSynthesizer implements Synthesizer {

    static final Soundbank BUILT_IN = soundbank("Built-in");

    private final FakeMidiDevice device = FakeMidiDevice.output("Gervill");
    private final List<Soundbank> loaded = new ArrayList<>(List.of(BUILT_IN));

    static Soundbank soundbank(String name) {
        return new Soundbank() {
            @Override public String getName() { return name; }
            @Override public String getVersion() { return "1"; }
            @Override public String getVendor() { return "test"; }
            @Override public String getDescription() { return name; }
            @Override public SoundbankResource[] getResources() { return new SoundbankResource[0]; }
            @Override public Instrument[] getInstruments() { return new Instrument[0]; }
            @Override public Instrument getInstrument(Patch patch) { return null; }
        };
    }

    List<Soundbank> loaded() {
        return List.copyOf(loaded);
    }

    @Override public Info getDeviceInfo() { return device.getDeviceInfo(); }
    @Override public void open() throws MidiUnavailableException { device.open(); }
    @Override public void close() { device.close(); }
    @Override public boolean isOpen() { return device.isOpen(); }
    @Override public long getMicrosecondPosition() { return -1; }
    @Override public int getMaxReceivers() { return device.getMaxReceivers(); }
    @Override public int getMaxTransmitters() { return device.getMaxTransmitters(); }
    @Override public Receiver getReceiver() throws MidiUnavailableException { return device.getReceiver(); }
    @Override public List<Receiver> getReceivers() { return device.getReceivers(); }
    @Override public Transmitter getTransmitter() throws MidiUnavailableException { return device.getTransmitter(); }
    @Override public List<Transmitter> getTransmitters() { return device.getTransmitters(); }
    @Override public int getMaxPolyphony() { return 64; }
    @Override public long getLatency() { return 0; }
    @Override public MidiChannel[] getChannels() { return new MidiChannel[0]; }
    @Override public VoiceStatus[] getVoiceStatus() { return new VoiceStatus[0]; }
    @Override public boolean isSoundbankSupported(Soundbank soundbank) { return true; }
    @Override public boolean loadInstrument(Instrument instrument) { return false; }
    @Override public void unloadInstrument(Instrument instrument) { }
    @Override public boolean remapInstrument(Instrument from, Instrument to) { return false; }
    @Override public Soundbank getDefaultSoundbank() { return BUILT_IN; }
    @Override public Instrument[] getAvailableInstruments() { return new Instrument[0]; }
    @Override public Instrument[] getLoadedInstruments() { return new Instrument[0]; }
    @Override public boolean loadAllInstruments(Soundbank soundbank) { return loaded.add(soundbank); }
    @Override public void unloadAllInstruments(Soundbank soundbank) { loaded.remove(soundbank); }
    @Override public boolean loadInstruments(Soundbank soundbank, Patch[] patches) { return false; }
    @Override public void unloadInstruments(Soundbank soundbank, Patch[] patches) { }
}
