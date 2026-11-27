# Piano Player: Architecture and Technical Plan

## 1. Overview

```
┌─────────────────────────── Desktop app (one package) ───────────────────────────┐
│                                                                                 │
│  ┌──────────── UI (Electron + TypeScript) ────────────┐                         │
│  │  screens · note display (canvas/SVG) · keyboard    │                         │
│  └───────────────▲───────────────────────┬────────────┘                         │
│                  │ events                │ requests      (Electron IPC)         │
│  ┌───────────────┴───────────────────────▼────────────────────────────────────┐ │
│  │          Java backend (child process, JSON lines on stdin/stdout)          │ │
│  │                                                                            │ │
│  │  api ──► engine (session, evaluation, playback scheduling)                 │ │
│  │   │        ▲            ▲                ▲                                 │ │
│  │   │     midi-jdk     generator        model ◄── musicxml ◄── import-midi   │ │
│  │   │   (in/out/synth) (random/chords)              ▲          (v2: import-pdf)│
│  │   └──► library (stored MusicXML songs, metadata, progress)                 │ │
│  └────────────────────────────────────────────────────────────────────────────┘ │
│                  ▲ USB MIDI (ALSA)                                              │
└──────────────────┼──────────────────────────────────────────────────────────────┘
              Digital piano
```

The backend does all the work: device handling, importing, generating notes, timing and scoring. The UI only displays and sends commands. Because of this, the UI can be swapped out, and the backend can be tested without any UI.

## 2. Technology choices

| Area | Choice | Why |
|---|---|---|
| Language (backend) | **Java 25 (LTS)** | Long-term support; modern features (records, sealed types, pattern matching, virtual threads) |
| Build | **Maven** (multi-module) | Your preference |
| MIDI | **`javax.sound.midi`** (built into Java) | Device listing, input/output, MIDI file reading, built-in synthesiser. Uses ALSA on Linux. |
| MusicXML | **ProxyMusic** (MusicXML 4.0 Java binding, used by Audiveris), with a hand-written StAX reader as a fallback | Full MusicXML support; the same library Audiveris uses, which makes v2 easier |
| Backend ↔ UI | **stdin/stdout pipes**, one JSON-RPC 2.0 message per line | No network port, only Electron can talk to the backend, the backend stops when Electron does, no HTTP server dependency |
| JSON | Jackson | Standard choice |
| Logging | SLF4J + Logback | Standard choice |
| Tests | JUnit 5, AssertJ | Standard choice |
| UI language | **TypeScript** | Type safety for the protocol with the backend |
| UI framework | **React + Vite** | Widely used, lots of tooling. Drawing done directly on canvas/SVG |
| Desktop window | **Electron** | A predictable Chromium engine, good canvas performance, mature Linux packaging |
| Notation (v1) | **VexFlow** or **OpenSheetMusicDisplay** (renders MusicXML directly, built on VexFlow) | Proper staff notation in canvas/SVG without writing a music engraver ourselves |
| Packaging | **jlink** (trimmed Java runtime) + **electron-builder** → AppImage / .deb | One self-contained package |
| OMR (v2) | **Audiveris** | Open-source Java OMR, outputs MusicXML |

### 2.1 Why a web UI in a desktop window, and not a browser

The UI is built with web technology (HTML5 canvas/SVG and a JS framework) but runs inside **Electron**, which is a desktop window with its own bundled browser engine. The user never opens a browser, and the app installs and starts like any desktop program.

At startup, Electron's main process launches the Java backend as a hidden child process and talks to it through the process's stdin and stdout (see §7). The renderer (the React UI) reaches the main process through Electron's IPC bridge. When the window closes, stdin closes and the backend stops.

Alternatives we considered:
- **Tauri**: much smaller package, but on Linux it relies on the system's WebKitGTK, whose canvas performance and behaviour vary between distributions.
- **JavaFX WebView**: one process and one language, but its WebKit is older and debugging is weaker.

The backend doesn't depend on the window shell, so switching later only affects packaging.

### 2.2 jpackage vs jlink

`jpackage` is a JDK tool that bundles a Java app and a Java runtime into a Linux package (.deb/.rpm/app image). Because Electron is the app here, we only need its building block, **`jlink`**, which produces a trimmed-down Java runtime (~40–60 MB). electron-builder then packages that runtime, the backend JAR and the UI into one AppImage/.deb. jpackage is still useful for a backend-only build (for example, a headless/CLI test build).

## 2A. Code principles

The code follows **clean architecture** and the **SOLID** principles:

- **S, single responsibility:** each class has one reason to change. For example, `WaitEvaluator` only decides whether a target was played correctly; it doesn't schedule playback or send events.
- **O, open/closed:** new behaviour is added by adding implementations, not by editing switch statements. A new follow mode is a new `Evaluator`, a new song source is a new `ScoreImporter`, a new exercise is a new `ExerciseGenerator`.
- **L, Liskov substitution:** any implementation can replace another of the same interface. For example, the real clock and the manual clock are interchangeable, and so are the software synth and a hardware MIDI output.
- **I, interface segregation:** interfaces are small and focused, such as `MidiInputSource`, `MidiOutputSink` and `DeviceCatalog`, rather than one large `MidiService`.
- **D, dependency inversion:** the core (`pp-model`, `pp-engine`, `pp-generator`) defines the interfaces it needs, and the outer modules (`pp-midi-jdk`, `pp-musicxml`, `pp-api`) implement them. MIDI devices are reached only through the `MidiInputPort` / `MidiOutputPort` interfaces in `pp-midi`, which also has virtual devices for tests and for on-screen keyboard input. The core never imports `javax.sound.midi` or Jackson, and never touches stdin/stdout. Wiring happens in one place: `pp-app` (constructor injection, no DI framework).

Layers, from the inside out:

```
domain        pp-model                        pure types, no I/O
ports         pp-midi                         MIDI interfaces and virtual devices
application   pp-engine, pp-generator         use cases; define ports (interfaces)
adapters      pp-midi-jdk, pp-musicxml,       implement the ports
              pp-import-midi, pp-library, pp-api
bootstrap     pp-app                          wiring, main()
```

**Conventions**

- **Naming:** Maven group ID and base Java package are `bg.petarh.piano`. Each module's packages start with its JPMS module name (`bg.petarh.piano.model`, `bg.petarh.piano.midi`, `bg.petarh.piano.midi.jdk`, …).
- **No comments that repeat the code.** No Javadoc or comments on classes, methods or fields that only restate their name or signature. Names should explain what code does. A comment is only allowed to explain a non-obvious *why* (for example a workaround for a quirk of Java MIDI on Linux).
- **Value objects with rules** (`Pitch`, `KeyboardRange`, `Velocity`, …) are `final` classes with a private constructor and static factories (`of`, `parse`), so every instance is valid and there is only one way to create one.
- **Plain data with no rules of its own** (events, descriptors) uses `record`.
- Everything is immutable: `final` fields, no setters, read-only collections returned.
- **Packages by feature** (`model.pitch`, `model.keyboard`, `midi.input`, …). Implementation classes are package-private, and only the interfaces and value types are `public`.
- **Each Maven module has a `module-info.java`** that exports only its API packages.
- No `null` in public APIs: use `Optional` for return values, and empty collections instead of null.
- Small classes and methods.
- Every port (interface) has a test double, so the engine and generator are tested without any I/O.

## 3. Project structure (Maven)

One root parent POM with two sub-projects, `backend` and `frontend`. The backend is itself a parent POM whose modules keep the layers apart: a module can only use what it declares as a dependency.

```
piano-player/                      root parent pom: shared coordinates, Maven version check
├── backend/                       backend parent pom: Java 25, test libraries, plugins, internal versions
│   ├── pp-model/                  shared song model, no dependencies
│   ├── pp-musicxml/               MusicXML ⇄ model
│   ├── pp-midi/                   MIDI port interfaces, events, virtual devices
│   ├── pp-midi-jdk/               javax.sound.midi adapter (hardware, built-in synth)
│   ├── pp-import-midi/            MIDI file → MusicXML (quantise, split hands)
│   ├── pp-generator/              random notes and chords
│   ├── pp-engine/                 sessions, evaluation, playback scheduling
│   ├── pp-library/                song library, metadata, settings, progress
│   ├── pp-api/                    JSON-RPC messages, dispatcher, stdio transport
│   ├── pp-app/                    main(): wires everything together, CLI options
│   └── pp-import-pdf/             (v2) Audiveris integration
└── frontend/                      Electron + React + TypeScript; Maven runs npm via frontend-maven-plugin

```

`frontend/` is a Maven sub-project of the root POM (packaging `pom`). `frontend-maven-plugin` installs a pinned Node LTS (`node.version`) into `frontend/target/node`, runs `npm ci` in `initialize`, `npm run build` in `compile` (`tsc --noEmit` and `vite build` into `dist/renderer`) and `npm test` (Vitest with jsdom) in `test`. All npm versions are pinned exactly in `package.json` and locked in `package-lock.json`. Frontend code lives in feature folders under `src/` (for example `src/tempo/`), with each test file next to its code (`*.test.ts`).

Allowed dependencies (arrows point to what a module may use):

```
pp-app → pp-api → pp-engine → pp-model
                  pp-engine → pp-midi → pp-model
pp-app → pp-midi-jdk → pp-midi
                  pp-engine → pp-generator → pp-model
         pp-api → pp-library → pp-musicxml → pp-model
                  pp-library → pp-import-midi → pp-musicxml
```

`pp-model` has no dependencies. `pp-engine` never depends on the network layer (`pp-api`).

## 4. Song model (`pp-model`)

Implemented so far: `model.pitch` (`Pitch`), `model.keyboard` (`KeyboardRange` with `parse("C4-C6")` and `parseKeyboard("88" or "F1-F7")`, `KeyboardPreset` with `withKeys(n)`), `model.time`, `model.key`, `model.note` and `model.chord`.

`model.time`:
- `Fraction`: exact musical time in whole notes. Always reduced, with a positive denominator. Overflow raises `ArithmeticException` instead of wrapping around.
- `Tempo`: BPM (20–240) plus a beat unit (quarter note by default). `nanosFor(duration)` converts musical time to nanoseconds, rounded to the nearest nanosecond. `adjustedBy` keeps the result within range.
- `TempoMarking`: the eight presets and their BPM ranges (TMP-1/2).
- `TempoMap`: converts between musical position and time across tempo changes (`nanosAt(position)`, `positionAt(nanos)`, `tempoAt(position)`). `withChangeAt(position, tempo)` returns a new map and drops any later changes. Positions before 0 (the count-in) use the first tempo. `Tempo.durationOf(nanos)` and `Fraction.floor()/ceil()` support it.
- `NoteValue`: whole, half, quarter, eighth; `fillsMeasureOf(meter)` and `notesPerMeasure(meter)`.
- `TimeSignature`: beats (1–32) over a note value (1, 2, 4 … 64), parsed from "6/8". `measureLength()` in whole notes. Compound meters (6/8, 9/8, 12/8, 6/4) are counted in dotted beats: `countedBeat()` is 3/8 and `beatsPerMeasure()` is 2 for 6/8. `PRACTICE_METERS` holds the four offered in random mode (2/4, 3/4, 4/4, 6/8).

`model.key`:
- `KeySignature`: number of fifths (−7 flats … +7 sharps) plus `KeyMode` (major/minor), as MusicXML stores it. Created from a tonic name (`major("Bb")`, `minor("F#")`) or parsed from a key name (`parse("F#m")`, `parse("Bb")`). `contains(Pitch)` tells whether a note belongs to the key (natural minor uses the notes of its relative major); `usesFlats()` decides sharp or flat spelling later.

`model.note`:
- `Hand`: `RIGHT` plays staff 1, `LEFT` plays staff 2. `portionOf(range)` splits a range at middle C (right: C4 and up, left: below C4); the generators use it when both hands practise, and MIDI import will use it as the default hand split (MID-5).
- `NoteEvent` record: `Pitch`, `start` and `duration` (`Fraction`, in whole notes) and `Hand`. The start can't be negative and the duration must be positive. Voice, measure, spelling and ties are added with MusicXML in v0.8; random practice doesn't need them.
- `Target`: the notes that start together (a single note or a chord), sorted low to high. `pitches()` / `pitchesFor(hand)` give what must be pressed; the same pitch in both hands counts once. `duration()` is the longest note.
- `Targets.from(notes, hands)`: groups notes by start time into targets in time order, keeping only the hands being practised.

`model.chord`:
- `ChordType`: major, minor, diminished, augmented, major 7, minor 7, dominant 7, as semitones above the root. Each has a code for the CLI (`maj`, `min`, `dim`, `aug`, `maj7`, `min7`, `dom7`) and a symbol for names (`""`, `m`, `dim`, `aug`, `maj7`, `m7`, `7`).
- `Inversion`: root position, 1st, 2nd, 3rd (3rd only for 7th chords). `ChordType.voicing(inversion)` gives the semitones above the root for an inversion.
- `Chord.of(root, type, inversion)`: the inversion moves the lowest notes up an octave, so C major 1st inversion on C4 is E4 G4 C5. `name(key)` gives names like "Dm/F". The root is spelled with sharps or flats as the key prefers; the other notes are spelled by letter (a 3rd is two letters above the root), so B major 1st inversion is "B/D#", not "B/Eb".
- Package-private `ChordSpelling` does the letter arithmetic.

The rest of this section is still planned.

These are the in-memory types every part of the app works with. MusicXML is the format stored on disk; this model is what the code uses.

```java
record Score(Metadata meta, List<Part> parts, TempoMap tempoMap,
             List<TimeSignatureChange> timeSigs, List<KeySignatureChange> keySigs,
             List<Measure> measures) {}

record Part(String id, String name, Instrument instrument, int staffCount,
            List<NoteEvent> notes) {}

record NoteEvent(int pitch,            // MIDI note 0–127
                 Fraction start,       // musical position, in whole notes from the start
                 Fraction duration,
                 int staff,            // 1 = upper / right hand, 2 = lower / left hand
                 int voice,
                 int measureIndex,
                 Spelling spelling,    // e.g. C#4 vs Db4, used for display
                 boolean tieStart, boolean tieStop) {}
```

- **Musical time** (`Fraction`, for example 3/8 of a whole note) is what's stored. The **`TempoMap`** converts musical time to milliseconds and back. That makes tempo scaling and Wait mode simple, and avoids rounding drift.
- **`KeyboardRange`** (lowest and highest MIDI note) is a model type. It is passed to the generator (to limit the notes it produces), to the session (to check songs are playable, spec KEY-7) and to the UI (to draw the keyboard).
- **`Tempo`** is stored as BPM plus a beat unit. A session's tempo is either an absolute BPM (random mode) or a scaling factor applied to the song's tempo map (songs). Both are shown in the UI as BPM.
- **Hand = staff:** staff 1 is the right hand, staff 2 the left hand. This matches how MusicXML describes piano parts.
- The engine flattens a part into a list of **`Target`s**, where each target is the group of notes that start at the same time (a single note or a chord), with tied notes merged.

## 4B. Generator (`pp-generator`)

Depends only on `pp-model`. Exported package `generator.notes`:
- `RandomNotesSettings` record: hands, practice range, keyboard range, key, note value, time signature, `Level` and `RandomNotesOptions` (a shorter constructor uses the defaults). `playsAccidentals()` / `playsMixedNoteValues()` say whether an option applies at the chosen level.
- `RandomNotesOptions` record: "allow sharps and flats" (from level 4), "mixed note values" (from level 3) and the chord types used at level 5 (default major and minor). `defaults()` plus `with…` methods. It is checked when created: the range is limited to the keyboard (`playableRange()`, KEY-6), the note value must fill the measure, and each hand needs at least two notes of the key.
- `RandomNotesGenerator`: `withSeed(settings, seed)` or `withRandomSeed(settings)` (the seed is kept in `seed()`, so any exercise can be replayed). `nextMeasure()` returns the next measure of `NoteEvent`s in time order, with start times following on from the previous measure, so it never runs out. Below level 5 the hands take turns each measure; at level 5 every hand gets its own line in every measure. The engine pulls measures ahead of time (look-ahead) and turns them into targets with `Targets.from`.
- `SplittableRandom` is used because its algorithm is fixed, so a seed gives the same notes on every JDK.
- Package-private: `HandPitches` (the notes a hand may play; with both hands, the right hand plays middle C and above and the left hand below it), `MelodicLine` (picks each next note, following the level's rules), `Harmony` (adds an interval or a triad to a note from level 3, or a chord of a chosen type at level 5) and `Rhythm` (fills a measure with note values).

Exported package `generator.chords` (chord practice, spec 4.3):
- `ChordSettings` record: hands, range, keyboard, key, chord types, inversions, note value, time signature. Checked when created like `RandomNotesSettings`, and each hand must have at least one chosen chord that fits completely in its range.
- `RandomChordGenerator`: same seed handling as the notes generator. `nextMeasure()` returns `ChordEvent`s, one chord per note value. Roots are notes of the key; the chord's other notes may have accidentals (D major in C major has F#). The same chord never comes twice in a row. With both hands, the hands take turns each measure.
- `ChordEvent` record: `Chord`, start, duration, hand; `notes()` turns it into `NoteEvent`s, which `Targets.from` groups back into one target per chord.
- Package-private `ChordCandidates`: every chord (root in key × type × inversion) whose notes all fit the hand's range.

## 4A. MIDI layer (`pp-midi`, `pp-midi-jdk`)

The rest of the backend only uses the interfaces in `pp-midi`. `javax.sound.midi` appears only in `pp-midi-jdk`.

**`pp-midi` (ports and virtual devices)**

| Package | Contents |
|---|---|
| `midi.device` | `DeviceId`, `DeviceDescriptor`, `DeviceUnavailableException` |
| `midi.value` | `Velocity` (1–127), `MidiChannel` (0–15) |
| `midi.input` | `MidiInputPort`, `InputConnection`, `InputListener`, `InputEvent` (`NoteOn`, `NoteOff`, `SustainPedal`), `TimestampSource` |
| `midi.output` | `MidiOutputPort`, `OutputConnection`, `OutputEvent` (`NoteOn`, `NoteOff`, `SustainPedal`, `AllNotesOff`) |
| `midi.virtual.input` | `VirtualKeyboard`: controller (`press` / `release` / `sustain`) plus its `MidiInputPort`. Used for testing and for on-screen keyboard input (DEV-7). |
| `midi.virtual.output` | `SilentOutput`: a `MidiOutputPort` that records sent events instead of playing them |

**`pp-midi-jdk` (hardware adapter)**

- **One public entry point:** `JdkMidi.inputPort()` / `JdkMidi.outputPort()`. Every other class is package-private, and the `message` package isn't exported.
- **Device catalog:**
  - Re-scans `MidiSystem` on every call, so hot-plugged devices appear.
  - Leaves out sequencers.
  - Devices with transmitters are inputs; devices with receivers are outputs, including the built-in synthesizer **Gervill**.
  - IDs are `jdk:<device name>`, with `#2`, `#3` added when names repeat. On Linux the name includes the ALSA port (for example `[hw:2,0,0]`).
- **Input:**
  - `InputMessageDecoder` maps Note On to `NoteOn`, Note On with velocity 0 or Note Off to `NoteOff`, and CC 64 to `SustainPedal` (down at 64 or more). Everything else is dropped.
  - The MIDI channel is ignored on input.
  - Each event is timestamped with `TimestampSource` when it arrives (DEV-8). The device's own timestamp isn't used.
- **Output:**
  - `OutputMessageEncoder` sends events immediately. Velocity is passed through unchanged. `SustainPedal` becomes CC 64 with value 127 (down) or 0 (up).
  - Closing an output first sends All Notes Off on all 16 channels (DEV-10).
- **Resource handling:**
  - If anything fails after `device.open()`, the device is closed again.
  - Connections close their parts with `try (device; transmitter/receiver; …)`, so every part is released even if one of them throws.
- **Tests** use `FakeMidiDevice`, `FakeTransmitter` and `FakeReceiver`. A smoke test checks the real `MidiSystem` (listing only, no sound card needed).

## 5. Engine (`pp-engine`)

The `pp-engine` module exists now, with a first use case: `engine.keyboard.KeyboardRangeDetection`. It's a small state machine (`AwaitingLowest` → `AwaitingHighest` → `Detected`) that is fed `InputEvent`s and only reacts to key presses. Pressing the first key again is ignored, and the order of the two keys doesn't matter. The CLI uses it now, and the device setup screen will later (KEY-3). It recognises a preset with `KeyboardPreset.matching(range)`.

Also implemented:
- `engine.time`: `Clock` (`nowNanos()`), `SystemClock` (`System.nanoTime`), `ManualClock` (moved forward by tests, never back) and `Stopwatch` (start, pause, resume; paused time isn't counted; `elapsedAt(timestamp)` for a moment in the past, such as a key press). Every engine test uses `ManualClock`, so none of them depends on real time.
- `engine.targets`: `TargetSource` (`next()`, `peek(count)`, `limitedTo(count)` for a session of N notes).
  - `TargetSource.of(list)` serves a fixed list (songs, tests).
  - `TargetSource.fromMeasures(supplier)` wraps a generator's `nextMeasure` and turns each measure into targets with `Targets.from`. It always holds the current measure plus 4 more (spec 4.4 look-ahead), so endless sessions never wait for the generator. The engine takes a plain `Supplier`, so it doesn't depend on `pp-generator`; `pp-app` connects them.
- `engine.session`: `Session` is the state machine `READY → COUNT_IN → RUNNING ⇄ PAUSED → FINISHED`.
  - `start()` takes the first target and runs the count-in from `SessionTiming` (tempo, time signature, 0–2 bars). `tick()` ends the count-in once its time has passed.
  - `advance()` moves to the next target (the evaluators will call it) and finishes after the last one. `stop()` finishes from any state.
  - `elapsedNanos()` counts running time only, without the count-in or pauses.
  - `positionNanosAt(timestamp)` is one timeline for the whole session: negative during the count-in, 0 at the first downbeat, and standing still while paused. Running time starts at the exact end of the count-in, not at the tick that notices it.
  - An action that doesn't fit the state throws `IllegalStateException`. The session isn't thread-safe, by design: it is only used from the session thread (§5.3).
- `engine.evaluation`: `Evaluator.onInput(InputEvent)` returns `Evaluation`s (sealed: `CorrectNote`, `WrongNote`, `TargetCompleted`).
  - `WaitEvaluator` implements Wait mode (spec 3.2). It tracks the keys held down and the press time of each target key counted so far. A target is complete when all its keys are counted and they are either all held or were pressed within the chord window (default 300 ms). It then calls `session.advance()`.
  - It counts `completedTargets()` and `wrongKeys()` for the results.
  - `TempoEvaluator` implements Tempo mode. A note is expected at `tempo.nanosFor(note.start())` of session running time (`Session.runningNanosAt(timestamp)`, so the count-in and pauses are left out). A press is matched to the nearest unmatched note of the same pitch whose `TimingWindows` (default ±50/±100/±150 ms) contain it, and graded `PERFECT`/`GOOD`/`OK` as `TimedNote` with its offset; otherwise it is a `WrongNote`. `onTick(now)` moves the session past each target whose widest window has closed, reporting unmatched notes as `MissedNote`, so the session finishes only after the last window. It counts `gradeCount(grade)`, `misses()` and `wrongKeys()`.
  - Tempo changes (TMP-3): `Session.changeTempo(tempo)` (while running or paused) adds a change to the session's `TempoMap` at the next counted beat. `Session.nanosAt(position)` is what the `TempoEvaluator` and the `Metronome` use, so expected note times and clicks follow the change. The CLI has no way to change tempo live; the API will call it.
- `engine.metronome`: `Metronome` is a `SessionListener` that clicks on every counted beat (`TimeSignature.countedBeat()`, so 6/8 clicks twice a bar), with an accent on each bar's first beat, from the first count-in beat on. If a tick comes late it clicks once for the latest beat instead of a burst. `MidiClicks` plays the clicks on General MIDI percussion (channel 10: high wood block for accents, low wood block for the other beats) through any `OutputConnection`. Clicks are sent from the session thread's tick, so they may be up to one tick interval late (5 ms in the CLI); scheduling ahead of time comes with the playback scheduler.
- `engine.run`: running a session.
  - `Results` is a `SessionListener` that counts evaluations. Its `summary()` is a `Summary` record: whether the session was timed, completed targets, correct notes, wrong keys, perfect/good/ok, missed, and accuracy % (spec 3.4: correct ÷ (correct + wrong) in Wait mode, matched ÷ expected in Tempo mode). The CLI prints it and the API sends it.
  - `Practice.prepare(targets, timing, mode, clock, listener, clicks)` is the one place a practice run is assembled: `Session`, the evaluator for the `FollowMode` (`WAIT` → `WaitEvaluator`, `TEMPO` → `TempoEvaluator`), an optional `Metronome`, the `SessionDriver` and the `SessionThread`. Its `input()` is the MIDI listener; `start`, `pause`, `resume`, `changeTempo` and `stop` are queued onto the session thread. The CLI uses it, and the API will. `elapsedNanos()` is meant to be read once the session has finished.
  - `SessionDriver` connects a `Session`, an `Evaluator` and a `Clock`. `start`, `handle(InputEvent)`, `tick`, `pause`, `resume` and `stop` drive them, and a `SessionListener` hears `onStateChanged`, `onTargetChanged` (only on real changes), every `onEvaluation`, and `onPosition` on start and on every tick while counting in or running. `SessionListener.all(list)` passes everything to several listeners. It is synchronous, so it is tested with `ManualClock`.
  - `SessionThread` is the session thread of §5.3. As an `InputListener`, it only queues events from the MIDI thread. One daemon thread runs the queued actions, ticks the driver every interval (for count-in and misses), and completes the future returned by `start()` when the session finishes. `pause()`/`resume()` are queued too (and ignored in the wrong state); `close()` stops the session and waits for the thread.

The rest of this section is still planned.

### 5.1 Parts of the engine

- **`Clock`**: a monotonic clock (`System.nanoTime`) that can be paused, plus tempo scaling. Tests use a manually controlled clock, which makes every engine test deterministic.
- **`Session`**: holds the state of one practice run. It is a state machine: `READY → COUNT_IN → RUNNING ⇄ PAUSED → FINISHED`.
- **`Evaluator`**: one implementation per follow mode (`WaitEvaluator`, `TempoEvaluator`, `NoScoreEvaluator`). Each one takes in key-press events and the clock, and outputs evaluation events.
- **`TargetSource`**: provides the targets either from a `Score` (songs) or from the `Generator` (random mode, endless, with look-ahead).
- **`PlaybackScheduler`**: plays the accompaniment, the hand you aren't playing, and the metronome to the output device, a little ahead of time. In Wait mode it is gated, meaning it only moves forward when the evaluator allows.

### 5.2 Event flow

```
piano ─► midi-jdk (timestamp) ─► InputEvent ─► Session ─► Evaluator ─► EvaluationEvent ─► api ─► UI
                                                │
                                                └──► PlaybackScheduler ─► midi-jdk out / synth
```

Every input event carries the backend timestamp from when it arrived (spec DEV-8). The evaluator only works with that timestamp.

### 5.3 Threads

- MIDI input callbacks (from `javax.sound.midi`) only put events on a queue. They do no other work.
- One **session thread** per session handles input events and clock ticks one at a time, so the session state needs no locking.
- The playback scheduler runs on its own thread and always schedules about 200 ms ahead.
- Events to the UI are written by a separate writer thread from a queue, so a slow UI can never block the session.

## 6. MIDI → MusicXML conversion (`pp-import-midi`)

1. Read the `Sequence` and collect note on/off pairs for each track and channel (taking the sustain pedal into account for note lengths only if the user turns that on).
2. Build the tempo map, time signatures and key signatures. If the key is missing, estimate it (Krumhansl-Schmuckler algorithm).
3. Quantise note starts and lengths to the grid, optionally detecting triplets.
4. Group into parts, and split piano parts into two staves (spec MID-5).
5. Split notes into measures and add ties where notes cross a bar line; assign voices; choose sharp or flat spelling based on the key.
6. Write the result with `pp-musicxml` and run the same checks used when opening any MusicXML file.

The conversion is tested with **golden files**: known MIDI files whose expected MusicXML output is stored and compared.

## 7. API (backend ↔ UI)

**Transport: stdin/stdout pipes.** Electron's main process starts the backend (`java … --api`) as a child process. Every message is one line of JSON (UTF-8, ending with `\n`): Electron writes requests to the backend's stdin and reads responses and events from its stdout. stderr is for logs only, and nothing else may be written to stdout. When stdin reaches end-of-file (Electron exited or closed it), the backend stops the session, closes the MIDI devices and exits. The backend opens no network port, so no other program can talk to it and no token is needed.

The renderer never talks to the backend directly. A small preload script exposes `request(method, params)` and `onEvent(listener)` through Electron's `contextBridge`, and the main process passes messages through.

**Protocol: JSON-RPC 2.0.**
- A **request** has an `id`, a `method` and `params`, and gets exactly one **response** with the same `id`: either `result` or `error` (`code`, `message`).
- An **event** (a JSON-RPC notification, without `id`) is pushed by the backend at any time: key presses, evaluations, beats, playhead position, session state, device changes.
- Methods are grouped by area: `devices.*`, `keyboard.*`, `settings.*`, `session.*` (start, pause, resume, stop, changeTempo), later `library.*` and `import.*`.

Sample messages:
```json
{"jsonrpc":"2.0","id":7,"method":"session.start","params":{"mode":"WAIT","exercise":{"type":"random","level":2}}}
{"jsonrpc":"2.0","id":7,"result":{"sessionId":"s1","seed":42}}
{"jsonrpc":"2.0","method":"input","params":{"pitch":60,"velocity":72,"on":true,"t":123456}}
{"jsonrpc":"2.0","method":"eval","params":{"result":"TIMED","pitch":60,"grade":"GOOD","offsetMs":-37}}
{"jsonrpc":"2.0","method":"session.state","params":{"state":"FINISHED","summary":{}}}
```

**Structure of `pp-api`:**
- **Messages and dispatcher:** the request and event types and a dispatcher that routes a request to its handler. This part doesn't know how bytes travel.
- **Transport adapter:** reads lines from an `InputStream` and writes lines to an `OutputStream`. In production these are `System.in` / `System.out`; in tests they are in-memory streams, so contract tests run a whole session without a process. Another adapter (for example a Unix socket for a debug tool) could be added later without changing the rest.

The message types are written once in Java, and the matching TypeScript types are generated from them, so the two sides can't drift apart.

Implemented so far (`pp-api`, using Jackson 3):
- `api.rpc.RpcServer`: `register(method, ParamsType.class, handler)` turns `params` into the handler's record (`Void.class` for none) and its return value into `result`.
  - Requests without `id` are run without a response.
  - Errors use the JSON-RPC codes: `-32700` parse error, `-32600` invalid request, `-32601` method not found, `-32602` invalid params (wrong shape, or an `IllegalArgumentException` from the model, with its message), `-32603` internal error.
  - Handlers can throw `RpcException` with an app code (`-32001` session error, `-32002` device error).
  - `notification(method, params)` writes an event.
- `api.PianoApi.register(server, inputs, outputs)` registers every method; `pp-app` calls it. Results are API records (in packages opened to Jackson), never the internal types, so the JSON format is under the API's control. Methods so far:
  - `devices.list` → `{"inputs":[{"id","name","description"}], "outputs":[…]}`.
  - `settings.get`, `settings.update` (through `api.settings.SettingsService`, which holds the current `Settings` and saves every change to the `SettingsStore`).
  - `session.start`, `session.pause`, `session.resume`, `session.changeTempo`, `session.stop`, plus the events `session.state`, `session.target`, `eval`, `input`, `beat` and `session.summary`. The full reference is in `docs/05-api.md`.
- `api.session.SessionMethods` keeps at most one active session. `PracticePlan` turns the flat `session.start` params into the model types, using the same parsers and defaults as the CLI, and builds the generator, the targets and the chord names. Bad values become -32602 with the model's message, and a missing device becomes -32002. The engine's `Practice` runs the session. `SessionEvents` (a `SessionListener`) and the input listener send events, and `Results` gives the summary. When the session finishes, the input and metronome output are closed, `session.summary` is sent, and a new session can start. `PianoApi.close()` (at end of input) stops a running session. JSON leaves out fields without a value. Sessions read their default tempo (per mode) and keyboard (per input) from the settings, and save the input and tempo when they start and when the tempo changes. The input listener moves every key press earlier by the latency offset (`LatencyOffset`) before the evaluator sees it. `pp api --settings <file>` chooses the file (default: `SettingsLocation.defaultSettingsFile()`). `api.devices.DeviceWatcher` re-lists the devices every second (started by `pp api` through `PianoApi.startWatchingDevices`). When the lists change it sends `devices.changed`, and it tells `SessionMethods.inputsChanged`. If the active session's input is gone, that pauses the session, closes the connection and sends `session.inputLost`. When the input is listed again, it reopens it with the same listener and sends `session.inputRestored`; the user then resumes (DEV-5).
- `pp api` (in `pp-app`) serves the API on stdin/stdout until end of input. This is the command Electron starts. `CliEnvironment` has `stdin`/`stdout` streams so tests use in-memory ones.
- `api.stdio.StdioTransport.run(in, out)` reads one request per line until end of input, skipping blank lines. A writer thread sends responses and events in order, so `events()` (an `EventSink`) can be used from any thread, for example the session thread. At end of input it runs the `onEndOfInput` action (stop the session, close devices), sends what's queued, and stops. Events after that are dropped.

`pp-app` also has a **headless CLI** built with picocli subcommands (for example `pp devices`, `pp monitor <device>`, `pp random --hands=right`). `bin/pp` runs it on the module path. `PianoCli.create(CliEnvironment, out, err)` builds it from a `CliEnvironment` (input port, output port, clock, `Lifetime`), so CLI tests run against the virtual devices with a controlled clock. Long-running commands hand their open connections to `Lifetime`. `keepOpenUntilStopped` waits for Ctrl+C (`monitor`, `echo`). `keepOpenUntil(resource, done)` waits until a task completes (`detect-range`, `random`, `chords`). In production (`UntilShutdown`), a shutdown hook closes the connections if Ctrl+C comes first, so outputs always get their All Notes Off. In tests a scripted lifetime plays some key presses and returns. A missing device is reported as a one-line error with exit code 1, not a stack trace. `pp echo` closes the input before the output, so no event reaches an output that is already closed; the output then sends All Notes Off. It can be used to test the backend before the UI exists, and it is what Phase 1–3 testing relies on.

`pp random` takes the shared practice options (`PracticeOptions`, a picocli mixin: input, hands, mode, tempo, key, range, keyboard, note value, time signature, number of targets, count-in, seed) plus level, `--accidentals` and `--mixed`. Text values are read by `PracticeConverters`, which call the model's parsers (`KeySignature.parse`, `KeyboardRange.parse`, `KeyboardRange.parseKeyboard`, `TimeSignature.parse`, `ChordType.forCode`), the same ones the API uses. Settings that can't be played (an invalid tempo, or a range without enough notes) become a usage error with exit code 2. `PracticeRunner` builds the targets from `TargetSource.fromMeasures(generator::nextMeasure)` (limited to `--notes`, or endless with 0) and runs them with `Practice`, whose input listener receives the MIDI input. `app.practice.PracticePrinter` is the `SessionListener`: it prints `Play: …`, one line per press (`ok`, `wrong`, `perfect C4 12 ms late`, `missed`) and state changes. The closing summary comes from the engine's `Results` and is turned into text by `PracticeSummary`. `--metronome <output>` (e.g. `jdk:Gervill`) adds a `Metronome` with `MidiClicks` on that output for the count-in and the whole session. The output closes last. `pp chords` uses the same options plus `--types`, `--inversions` and `--show both|name|notes`. It remembers each generated `ChordEvent` by start time, so its target can be printed as `Dm/F  (F4 A4 D5)`, as the name only, or as the notes only (spec 4.3).

## 8. Storage

```
~/.config/piano-player/settings.json          devices, keyboard range for each device, latency offset,
                                               last tempo for each mode, UI preferences
~/.local/share/piano-player/library/<id>/      score.musicxml, meta.json, import-report.json
~/.local/share/piano-player/progress.db        (v1) session results, SQLite
```

Implemented so far (`pp-library`, settings part):
- `library.settings.Settings` is an immutable value. It holds the input and output device ids, a keyboard range for each device (KEY-4), the last tempo for each follow mode (TMP-7) and a latency offset of ±500 ms (DEV-9). Changes go through `with…` methods.
- `SettingsStore` (`load`/`save`) is implemented by `JsonSettingsFile`:
  - **Saving** writes pretty-printed JSON to a temporary file in the same folder and then moves it into place. A crash can't leave a half-written file.
  - **Loading** ignores unknown fields and values it can't use, such as an unreadable range or an out-of-range offset.
  - **A corrupt file** is moved to `settings.json.bad`, and loading returns the defaults, so a broken file never stops the app from starting.
- `SettingsLocation.defaultSettingsFile()` follows XDG: `$XDG_CONFIG_HOME/piano-player/settings.json`, otherwise `~/.config/piano-player/settings.json` (NF-7).

## 9. Testing strategy

| Level | What | How |
|---|---|---|
| Unit | model, quantisation, generator, evaluators | JUnit, manual clock, seeded generator |
| Golden | MIDI → MusicXML, MusicXML → model | stored inputs and expected outputs in `src/test/resources` |
| Engine scenarios | whole sessions from scripted input ("press C4 at 1000 ms…") | a scenario test runner that uses the manual clock |
| Device integration | real ALSA path | a virtual MIDI port (`snd-virmidi` kernel module plus `aplaymidi`/`amidi`) to send in recorded MIDI |
| API | JSON-RPC contract over stdio | In-memory streams in place of stdin/stdout, JSON snapshot tests |
| UI | components, protocol handling | Vitest, plus Playwright with Electron for smoke tests |
| Manual | with the real piano | a checklist for each phase (see `04-phases.md`) |

## 10. Risks

| Risk | Mitigation |
|---|---|
| Java MIDI on Linux: device names or hot-plugging behave inconsistently | Device scanning in the background, matching remembered devices by name, and early testing on your piano (Phase 0) |
| MIDI → notation produces messy rhythms | Adjustable quantisation, a preview step, golden-file tests on real songs |
| Delay between backend and UI | Scoring uses backend timestamps; pipes plus Electron IPC add well under 1 ms; measure in Phase 4 |
| Electron package size (~150 MB with the Java runtime) | Acceptable for a desktop app; Tauri is the fallback if size matters |
| Software synthesizer (Gervill) adds audio buffer latency (about 100 ms by default), so `echo` and playback may sound late | Measure with the piano. If noticeable, open Gervill with a smaller latency setting, or play through the piano's own sound engine (MIDI out) |
| Rendering notation (v1) | Use VexFlow/OSMD instead of writing our own engraver |
| OMR quality (v2) | Review screen; MusicXML from notation software stays the recommended way to add songs |
