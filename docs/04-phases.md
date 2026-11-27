# Piano Player: Development and Testing Phases

The backend comes first, and **random notes and chords come before songs**. Each phase ends with something you can run and check, plus automated tests. A phase is **done** when everything in its "Done when" list is true.

```
v0.5 Random PoC:  P0 Skeleton → P1 Engine+Generator → P2 API+Metronome → P3 UI PoC → P4 Packaging
v0.8 Songs:       P5 MusicXML → P6 MIDI→MusicXML → P7 Song sessions+Playback (backend+UI)
v1.0:             P8 Notation → P9 Full practice features → P10 Release hardening
v2.0:             P11 PDF/OMR
```

## Current status

| Phase | Status |
|---|---|
| P0 Skeleton and MIDI devices | **In progress.** Done: Maven build, `pp-model` (`Pitch`, `KeyboardRange`, presets), `pp-midi` (ports, events, virtual devices), `pp-midi-jdk` (input, output, device catalog). Remaining: `pp-app` CLI (`--list-devices`, `--monitor`, `--detect-range`, `--echo`), `snd-virmidi` integration test, manual test with the piano. |
| P1–P11 | Not started |

This table is updated in every PR that completes a deliverable.

---

## v0.5: Proof of concept, random notes and chords

### Phase 0: Project skeleton and MIDI devices
**Goal:** confirm the piano works with Java on Linux, set up the project structure.

Deliverables:
- Maven multi-module project (`pp-model`, `pp-midi`, `pp-midi-jdk`, `pp-app` to start), Java 25, a working build with JUnit.
- `pp-midi`: `MidiInputPort` / `MidiOutputPort` interfaces, `InputEvent` / `OutputEvent`, virtual input and output devices (testable with no hardware).
- `pp-midi-jdk`: `javax.sound.midi` implementation: list devices, open an input, timestamped `InputEvent` stream, open an output or the built-in synth.
- `KeyboardRange` with the presets (88/76/73/61/49/37/25) and custom ranges (spec 1A).
- CLI:
  - `pp --list-devices`
  - `pp --monitor <device>`: prints note on/off with timestamps and note names
  - `pp --detect-range <device>`: "press your lowest key, then your highest key" (KEY-3)
  - `pp --echo <in> <out>`: plays what you press through the synth

Testing:
- Unit: converting MIDI messages to `InputEvent` (including Note On with velocity 0 counting as Note Off, and the sustain pedal); presets and limits of `KeyboardRange`.
- Integration: a virtual MIDI port (`snd-virmidi`) with a recorded MIDI file sent in, checking that the events come out in the right order.
- **Manual:** connect your 88-key piano. Check that `--monitor` shows every key including A0 and C8, chords and fast repeated notes, and that `--detect-range` reports A0–C8.

Done when: `mvn verify` passes, and your piano's key presses show up in `--monitor` with plausible timestamps.

---

### Phase 1: Model, engine and generator (random notes and chords)
**Goal:** the core practice loop works headless (without a UI) for random notes and chords.

Deliverables:
- `pp-model`: `NoteEvent`, `Fraction`, `Tempo`, `TempoMap`, `Target`, `KeyboardRange`, time signatures, key signatures.
- `pp-generator`:
  - random notes (range limited to the keyboard range, key, hands, complexity levels 1–5, note value, time signature, seed)
  - **chord practice** (chord types, inversions, show or hide chord names)
  - look-ahead stream for endless sessions
- `pp-engine`: `Clock` (real and manual), tempo handling (BPM, beat unit, changing tempo mid-session), `Session` state machine, `WaitEvaluator`, `TempoEvaluator`, `NoScoreEvaluator`, results summary.
- CLI: `pp --random --hands=right --mode=wait --level=2 --tempo=90` and `pp --chords --types=maj,min --inversions=0,1 --mode=wait`. They print the target ("C4", "Dm/F") and the result of each press in the terminal.

Testing:
- Unit: generator is repeatable with a seed, **never produces a note outside the keyboard range** (property tests over all presets and random ranges), respects key and range, produces the right notes for each chord and inversion, and drops chords that don't fit in the range.
- Unit: tempo conversion (BPM ⇄ milliseconds for each beat unit), tempo changes taking effect from the next beat.
- Engine scenario tests (manual clock): correct and wrong keys in Wait mode, chord window, all timing grades in Tempo mode, misses, pressing the same key twice, changing tempo mid-session.
- **Manual:** play a 50-note random session and a chord session in the terminal using the piano.

Done when: scenario tests cover every rule in spec sections 3.2 and 4, and terminal sessions with the real piano feel right.

---

### Phase 2: Backend API, metronome and settings
**Goal:** the backend is complete for random mode and can be driven over the network.

Deliverables:
- Metronome and count-in through the synth or MIDI output, following the current tempo (TMP-6).
- `pp-api`: REST (devices, keyboard range, settings, random-session setup and control, tempo changes) and WebSocket events (input, evaluation, session state, beat ticks, device changes); localhost only with a token; message definitions with generated TypeScript types.
- `pp-library` (settings part only): settings saved to disk (devices, keyboard range for each device, latency offset, last tempo for each mode).
- Device hot-plugging (DEV-5).

Testing:
- Metronome scheduling tests with the manual clock, including tempo changes.
- API contract tests: JSON snapshots for each message type; a WebSocket client test that runs a whole random session and a chord session from start to results.
- Settings save and reload correctly.
- **Manual:** the metronome sounds steady at 40, 90 and 240 BPM; unplug and replug the piano during a session.

Done when: a scripted WebSocket client can run a complete random or chord session from start to results.

---

### Phase 3: UI proof of concept (random mode)
**Goal:** the full loop works with a window: piano → backend → feedback.

Deliverables:
- `frontend/`: Electron + React + TypeScript + Vite as a Maven sub-project (frontend-maven-plugin runs npm); Electron starts the backend as a hidden process.
- Screens: Home, Devices (input/output, live keyboard, **keyboard size presets, custom range, range detection**), Random-session setup (all settings in spec 4.1, including chord practice), Practice, Results.
- **Tempo control** used throughout (TMP-1/2): preset buttons, slider, number field, −/+ buttons, the matching marking name shown for the current value.
- **Simple note display** (no staff notation): lanes for right and left hand, a "now" line, note and chord names, green/red/grey feedback.
- On-screen keyboard drawing the configured range, showing pressed keys, target hints and feedback colours; virtual keyboard input (DEV-7).

Testing:
- Vitest: protocol client, tempo control logic (slider, field and presets stay in sync, values stay within 20–240), keyboard layout for each preset.
- Playwright + Electron smoke test: start the app, choose virtual input, set 61 keys, run a short random session and a chord session.
- **Latency check:** log the time from a key press arriving in the backend to it appearing in the UI; target under 20 ms (NF-1).
- **Manual:** full random and chord sessions with the piano at several tempos.

Done when: you can practise random notes and chords with the real piano using only the UI.

---

### Phase 4: v0.5 packaging
**Goal:** a self-contained Linux build.

Deliverables:
- jlink runtime, electron-builder → **AppImage** (and .deb).
- A single build command, or a wrapper script that runs `mvn package` and `npm run dist`.

Testing:
- Install and run on a clean machine or VM with no Java or Node installed.
- Startup time (NF-5).

**v0.5 milestone:** a self-contained app for practising random notes and chords.

---

## v0.8: Songs

### Phase 5: MusicXML reading
- `pp-model`: `Score`, `Part`, staves and hands, measures.
- `pp-musicxml`: MusicXML (and `.mxl`) → `Score` for the v1 subset (LIB-5), `Score` → MusicXML, import report.
- `pp-library`: song library folder, metadata, list and delete.
- Testing: 5–10 golden MusicXML files exported from MuseScore (simple melody, two-hand piece, chords, ties, triplets, key change, tempo change); round trip MusicXML → model → MusicXML → model gives the same model; broken input produces an import report instead of a crash.

### Phase 6: MIDI → MusicXML conversion
- `pp-import-midi`: architecture section 6, steps 1–6 (quantisation, tempo/time/key signatures, grouping into parts, hand splitting, ties).
- CLI: `pp --import song.mid --out song.musicxml`.
- Testing: 5–10 golden MIDI files (type 0 and 1, several instruments, a piano piece in one track, a piano piece with separate hand tracks); the same input always gives the same output; **manual** check of the converted files in MuseScore.

### Phase 7: Song sessions and playback (backend + UI)
- Engine: `TargetSource` from a `Score`, choice of part and hands, notes outside the keyboard range (KEY-7), tempo shown as BPM and as a percentage of the song (TMP-5).
- `PlaybackScheduler`: plays the accompaniment and the hand you aren't playing; gated in Wait mode; Listen and Play-along modes.
- UI: Library screen (import MusicXML/MIDI, list of parts, include/exclude), song session setup, song practice in the simple display.
- Testing: scheduler tests with the manual clock; scenario tests for song sessions; **manual** practice of a real song in all four follow modes.

**v0.8 milestone:** practise imported MusicXML and MIDI songs with the real piano.

---

## v1.0: First real version

### Phase 8: Sheet-music display
- Grand-staff rendering with VexFlow / OpenSheetMusicDisplay, highlighting the current position, scrolling or turning pages, feedback colours on the notes. Random mode also renders as notation.
- Users can switch between notation view and lane view.
- Testing: visual snapshot tests against the golden MusicXML files; manual readability check.

### Phase 9: Full practice features
- Full MIDI import screen (preview, quantisation and hand-split controls).
- Section selection and looping, all settings in spec 3.1, latency offset adjustment.
- Saved progress (SQLite), measures with the most mistakes, history for each song and exercise.
- Testing: scenario tests for looping and sections; migration tests for the progress database; manual checklist covering the whole specification.

### Phase 10: Release hardening
- Error handling and import reports shown in the UI, logs, crash recovery (restarting the backend if it stops).
- Performance with large scores; tested on 2–3 Linux distributions.
- User documentation.

**v1.0 milestone:** all v1 requirements in `02-specification.md` are met.

---

## v2.0

### Phase 11: PDF import (OMR)
- `pp-import-pdf`: run Audiveris (bundled), PDF → MusicXML → library.
- Review screen showing the recognised notation, with basic fixes.
- Testing: a set of test PDFs (exported from notation software and scanned), measuring how accurately notes are recognised.

---

## Ongoing practices (all phases)

- `mvn verify` and `npm test` pass before every merge.
- Each phase updates the manual test checklist in `docs/testing/` with what was tested on the real piano.
- The specification is updated whenever a decision changes during development.
