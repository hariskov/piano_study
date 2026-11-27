package io.github.hariskov.piano.midi.jdk;

import javax.sound.midi.MidiDevice;
import java.util.List;

@FunctionalInterface
interface MidiDeviceSource {

    List<? extends MidiDevice> devices();
}
