package io.github.hariskov.piano.midi.jdk;

import io.github.hariskov.piano.midi.device.DeviceDescriptor;
import io.github.hariskov.piano.midi.device.DeviceId;

import javax.sound.midi.MidiDevice;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

final class JdkDeviceCatalog {

    private static final String ID_PREFIX = "jdk:";

    private final MidiDeviceSource source;

    JdkDeviceCatalog(MidiDeviceSource source) {
        this.source = source;
    }

    List<DeviceDescriptor> inputs() {
        return descriptors(inputEntries());
    }

    List<DeviceDescriptor> outputs() {
        return descriptors(outputEntries());
    }

    Optional<Entry> findInput(DeviceId id) {
        return find(inputEntries(), id);
    }

    Optional<Entry> findOutput(DeviceId id) {
        return find(outputEntries(), id);
    }

    private List<Entry> inputEntries() {
        return entries(device -> device.getMaxTransmitters() != 0);
    }

    private List<Entry> outputEntries() {
        return entries(device -> device.getMaxReceivers() != 0);
    }

    private List<Entry> entries(Predicate<MidiDevice> direction) {
        Map<String, Integer> seenNames = new HashMap<>();
        List<Entry> entries = new ArrayList<>();
        for (MidiDevice device : source.devices()) {
            if (direction.test(device)) {
                MidiDevice.Info info = device.getDeviceInfo();
                int occurrence = seenNames.merge(info.getName(), 1, Integer::sum);
                String suffix = occurrence == 1 ? "" : " #" + occurrence;
                DeviceId id = DeviceId.of(ID_PREFIX + info.getName() + suffix);
                entries.add(new Entry(new DeviceDescriptor(id, info.getName(), info.getDescription()), device));
            }
        }
        return entries;
    }

    private static List<DeviceDescriptor> descriptors(List<Entry> entries) {
        return entries.stream().map(Entry::descriptor).toList();
    }

    private static Optional<Entry> find(List<Entry> entries, DeviceId id) {
        return entries.stream().filter(entry -> entry.descriptor().id().equals(id)).findFirst();
    }

    record Entry(DeviceDescriptor descriptor, MidiDevice device) {
    }
}
