# Phase 0: manual tests with the real piano

## 2026-11-27: Yamaha P-45 over USB, `pp monitor`

Setup: Yamaha P-45 (USB ID `0499:160f`, which ALSA reports as "Digital Piano"), Linux laptop, OpenJDK 25.
The device was listed as `jdk:Piano [hw:3,0,0]` for both input and output.

| Check | Result |
|---|---|
| Piano appears while the app is running (hot-plug) | ✅ Found by re-scanning, no restart |
| `pp devices` lists it as input and output | ✅ |
| Correct note names | ✅ C4, G3, A3, B3 as played |
| Full 88-key range | ✅ A0 and C8 both received |
| Every press has a release | ✅ |
| Fast repeated notes | ✅ About 70 ms apart, none lost |
| Sustain pedal | ✅ Down and up |
| Velocity, Touch = Fixed | ✅ Every note 64, as expected for Fixed |
| Velocity, Touch = Medium | ✅ 38–108, soft and hard clearly different |
| Stopping with SIGTERM/Ctrl+C | ✅ Shutdown hook closed the device, no process left behind |

### Findings

- **P-45 touch sensitivity:** hold `[GRAND PIANO/FUNCTION]` and press A2 (Fixed), A#2 (Soft), B2 (Medium) or C3 (Hard), in Yamaha's naming where middle C is **C3**. In our naming those keys are A3, A#3, B3 and middle C (C4). The recording confirms it: velocity became fixed at 64 straight after A3, and varied again after B3.
- **Settings keys are sent as notes:** keys pressed together with `[GRAND PIANO/FUNCTION]` are also sent over MIDI as ordinary notes (see spec DEV-12).

## 2026-11-27: Gervill output on the laptop (no piano)

A small program opened `jdk:Gervill` through `JdkMidi.outputPort()` and played C4–E4–G4–C5 at velocity 90.

| Check | Result |
|---|---|
| Gervill opens through the adapter | ✅ About 0.7 s the first time |
| Notes play on the laptop speakers | ✅ Heard by the user |
| Closes cleanly (All Notes Off, device released) | ✅ |

Still to check with the piano: `pp echo` from end to end, and whether Gervill's delay is noticeable.
