package bg.petarh.piano.api.sound;

import bg.petarh.piano.api.rpc.RpcException;
import bg.petarh.piano.api.rpc.RpcServer;
import bg.petarh.piano.api.settings.SettingsService;
import bg.petarh.piano.library.settings.Settings;
import bg.petarh.piano.midi.device.DeviceId;
import bg.petarh.piano.midi.device.DeviceUnavailableException;
import bg.petarh.piano.midi.output.MidiOutputPort;
import bg.petarh.piano.midi.output.OutputConnection;
import bg.petarh.piano.midi.output.OutputEvent;
import bg.petarh.piano.midi.value.Instrument;
import bg.petarh.piano.midi.value.Velocity;
import bg.petarh.piano.model.pitch.Pitch;

import java.time.Duration;
import java.util.List;

public final class SoundMethods {

    private static final List<Pitch> ARPEGGIO = List.of("C4", "E4", "G4", "C5").stream().map(Pitch::parse).toList();
    private static final Duration STEP = Duration.ofMillis(250);
    private static final Duration CHORD = Duration.ofMillis(900);
    private static final Velocity VELOCITY = Velocity.of(80);

    public record SoundTestParams(Integer instrument) {
    }

    private SoundMethods() {
    }

    public static void register(RpcServer server, MidiOutputPort outputs, SettingsService settings) {
        server.register("sound.test", SoundTestParams.class, Void.class, params -> {
            Settings saved = settings.current();
            Instrument instrument = Instrument.of(params.instrument() == null ? saved.instrument() : params.instrument());
            String id = saved.output().orElseThrow(() -> new RpcException(RpcException.DEVICE_ERROR,
                    "Choose a sound output on the Devices screen first"));
            OutputConnection output = open(outputs, id);
            Thread.ofVirtual().name("sound-test").start(() -> play(output, instrument));
            return null;
        });
    }

    private static OutputConnection open(MidiOutputPort outputs, String id) {
        try {
            return outputs.open(DeviceId.of(id));
        } catch (DeviceUnavailableException e) {
            throw new RpcException(RpcException.DEVICE_ERROR, e.getMessage());
        }
    }

    private static void play(OutputConnection output, Instrument instrument) {
        try (output) {
            output.send(new OutputEvent.ProgramChange(instrument, KeySound.CHANNEL));
            for (Pitch pitch : ARPEGGIO) {
                output.send(new OutputEvent.NoteOn(pitch, VELOCITY, KeySound.CHANNEL));
                Thread.sleep(STEP);
                output.send(new OutputEvent.NoteOff(pitch, KeySound.CHANNEL));
            }
            ARPEGGIO.forEach(pitch -> output.send(new OutputEvent.NoteOn(pitch, VELOCITY, KeySound.CHANNEL)));
            Thread.sleep(CHORD);
            ARPEGGIO.forEach(pitch -> output.send(new OutputEvent.NoteOff(pitch, KeySound.CHANNEL)));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
