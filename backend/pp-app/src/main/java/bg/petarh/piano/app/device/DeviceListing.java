package bg.petarh.piano.app.device;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.input.MidiInputPort;
import bg.petarh.piano.midi.output.MidiOutputPort;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class DeviceListing {

    private static final String INDENT = "  ";
    private static final int COLUMN_GAP = 3;

    private final MidiInputPort inputs;
    private final MidiOutputPort outputs;

    public DeviceListing(MidiInputPort inputs, MidiOutputPort outputs) {
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public List<String> lines() {
        List<DeviceDescriptor> inputDevices = inputs.devices();
        List<DeviceDescriptor> outputDevices = outputs.devices();
        int idWidth = Stream.concat(inputDevices.stream(), outputDevices.stream())
                .mapToInt(device -> device.id().value().length())
                .max()
                .orElse(0) + COLUMN_GAP;

        List<String> lines = new ArrayList<>();
        lines.add("Inputs:");
        lines.addAll(section(inputDevices, idWidth, "(none found - is the piano connected and switched on?)"));
        lines.add("Outputs:");
        lines.addAll(section(outputDevices, idWidth, "(none found)"));
        return lines;
    }

    private static List<String> section(List<DeviceDescriptor> devices, int idWidth, String whenEmpty) {
        if (devices.isEmpty()) {
            return List.of(INDENT + whenEmpty);
        }
        return devices.stream()
                .map(device -> INDENT + pad(device.id().value(), idWidth) + device.description())
                .toList();
    }

    private static String pad(String text, int width) {
        return text + " ".repeat(width - text.length());
    }
}
