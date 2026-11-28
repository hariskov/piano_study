package bg.petarh.piano.api.sound;

import bg.petarh.piano.api.PianoApi;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.settings.InMemorySettings;
import bg.petarh.piano.midi.output.OutputEvent;
import bg.petarh.piano.midi.value.Instrument;
import bg.petarh.piano.midi.value.MidiChannel;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.midi.virtual.input.VirtualKeyboard;
import bg.petarh.piano.midi.virtual.output.SilentOutput;
import bg.petarh.piano.model.pitch.Pitch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeySoundApiTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final MidiChannel KEYS = MidiChannel.of(0);
    private static final Pitch MIDDLE_C = Pitch.parse("C4");

    private final VirtualKeyboard keyboard = VirtualKeyboard.withClock(System::nanoTime);
    private final SilentOutput silent = SilentOutput.create();
    private final RpcServer server = new RpcServer();
    private final PianoApi api = PianoApi.create(server, keyboard.port(), silent.port(), System::nanoTime,
            (method, params) -> { }, new InMemorySettings(), keyboard);

    @AfterEach
    void closeApi() {
        api.close();
    }

    @Test
    void playsYourKeysInTheChosenInstrumentDuringASession() {
        chooseSound(56, true);
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"countIn\":0}");

        keyboard.press(MIDDLE_C, Velocity.of(70));
        call("session.stop", "{}");

        assertThat(keySounds()).startsWith(
                new OutputEvent.ProgramChange(Instrument.of(56), KEYS),
                new OutputEvent.NoteOn(MIDDLE_C, Velocity.of(70), KEYS));
    }

    @Test
    void keepsYourKeysSilentWhenKeySoundIsOff() {
        chooseSound(56, false);
        call("session.start", "{\"input\":\"virtual:keyboard\",\"exercise\":\"random\",\"countIn\":0}");

        keyboard.press(MIDDLE_C, Velocity.of(70));
        call("session.stop", "{}");

        assertThat(keySounds()).isEmpty();
    }

    @Test
    void playsYourKeysOnTheDevicesScreen() {
        chooseSound(19, true);
        call("devices.monitor", "{\"input\":\"virtual:keyboard\"}");

        keyboard.press(MIDDLE_C, Velocity.of(70));
        keyboard.release(MIDDLE_C);

        assertThat(keySounds()).containsExactly(
                new OutputEvent.ProgramChange(Instrument.of(19), KEYS),
                new OutputEvent.NoteOn(MIDDLE_C, Velocity.of(70), KEYS),
                new OutputEvent.NoteOff(MIDDLE_C, KEYS));
    }

    @Test
    void playsASampleOfAnInstrument() {
        chooseSound(0, false);

        assertThat(call("sound.test", "{\"instrument\":89}").get("result").isNull()).isTrue();

        Instant deadline = Instant.now().plus(Duration.ofSeconds(5));
        while (keySounds().stream().filter(OutputEvent.NoteOff.class::isInstance).count() < 4 && Instant.now().isBefore(deadline)) {
            Thread.onSpinWait();
        }
        assertThat(keySounds().getFirst()).isEqualTo(new OutputEvent.ProgramChange(Instrument.of(89), KEYS));
        assertThat(keySounds()).filteredOn(OutputEvent.NoteOn.class::isInstance).hasSizeGreaterThanOrEqualTo(4);
    }

    @Test
    void needsAnOutputToPlayASample() {
        JsonNode error = call("sound.test", "{\"instrument\":89}").get("error");

        assertThat(error.get("code").asInt()).isEqualTo(-32002);
        assertThat(error.get("message").asString()).isEqualTo("Choose a sound output on the Devices screen first");
    }

    private void chooseSound(int instrument, boolean keySound) {
        call("settings.update", "{\"output\":\"" + silent.device().id().value() + "\",\"instrument\":" + instrument
                + ",\"keySound\":" + keySound + "}");
    }

    private List<OutputEvent> keySounds() {
        return silent.sentEvents().stream().filter(event -> event.channel().equals(KEYS)).toList();
    }

    private JsonNode call(String method, String params) {
        String request = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}";
        return JSON.readTree(server.handle(request).orElseThrow());
    }
}
