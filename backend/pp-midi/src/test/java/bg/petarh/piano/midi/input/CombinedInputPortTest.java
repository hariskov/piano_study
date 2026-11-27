package bg.petarh.piano.midi.input;

import bg.petarh.piano.midi.device.DeviceDescriptor;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CombinedInputPortTest {

    private final VirtualKeyboard keyboard = VirtualKeyboard.withClock(() -> 0L);
    private final MidiInputPort empty = new MidiInputPort() {
        @Override
        public List<DeviceDescriptor> devices() {
            return List.of();
        }

        @Override
        public InputConnection open(DeviceId id, InputListener listener) {
            throw new DeviceUnavailableException(id);
        }
    };

    @Test
    void listsTheDevicesOfEveryPort() {
        MidiInputPort combined = MidiInputPort.combining(List.of(empty, keyboard.port()));

        assertThat(combined.devices()).extracting(device -> device.id().value()).containsExactly("virtual:keyboard");
    }

    @Test
    void opensTheDeviceOnThePortThatHasIt() {
        List<InputEvent> received = new ArrayList<>();
        MidiInputPort combined = MidiInputPort.combining(List.of(empty, keyboard.port()));

        combined.open(DeviceId.of("virtual:keyboard"), received::add);
        keyboard.press(Pitch.parse("C4"), Velocity.of(64));

        assertThat(received).hasSize(1);
    }

    @Test
    void reportsADeviceNoPortHas() {
        MidiInputPort combined = MidiInputPort.combining(List.of(empty, keyboard.port()));

        assertThatThrownBy(() -> combined.open(DeviceId.of("jdk:Missing"), event -> { }))
                .isInstanceOf(DeviceUnavailableException.class);
    }
}
