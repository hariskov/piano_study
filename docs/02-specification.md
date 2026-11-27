# Piano Player: Specification

Status: draft · Target: v0.5 → v1.0 (v2 items are marked)

Build order: random notes and chords (sections 1, 1A, 1B, 3.2, 4, 5) come first in v0.5. Songs (sections 2 and 3) come in v0.8.

The keywords **MUST**, **SHOULD** and **MAY** mean: required, recommended, and optional.

---

## 1. Devices (MIDI input/output)

| ID | Requirement |
|---|---|
| DEV-1 | The app MUST list all available MIDI input and output devices. |
| DEV-2 | The user MUST be able to choose one input device (the piano) and, optionally, one output device. |
| DEV-3 | Output choices MUST include the **built-in software synthesiser** and every hardware MIDI output (for example, the piano's own sound engine). |
| DEV-4 | The chosen devices MUST be remembered between sessions and reconnected automatically when they are available. |
| DEV-5 | The app MUST notice when a device is unplugged or plugged back in. It SHOULD reconnect without a restart. |
| DEV-6 | The app MUST show live input activity on the device screen (for example, a keyboard where pressed keys light up) so the user can check the connection. |
| DEV-7 | The app MUST offer **virtual keyboard input** (computer keyboard or mouse on the on-screen keyboard) for testing without a piano. |
| DEV-8 | Key presses MUST be timestamped by the backend when they arrive. All scoring uses the backend clock, so delays in the UI never affect scores. |
| DEV-9 | The app SHOULD offer a latency offset setting (in milliseconds) for Tempo mode. |
| DEV-10 | Closing an output device MUST first silence it (All Notes Off on every channel), so no note keeps sounding. |
| DEV-11 | Key velocity (how hard a key is pressed, 1–127) MUST be kept on every key press, and passed through unchanged when notes are played back. |
| DEV-12 | (v1) Some pianos send the keys used to change settings (for example FUNCTION + key on the Yamaha P-45) as ordinary notes. A session MUST offer a way to avoid these counting as wrong notes, for example by pausing the session. |

MIDI messages handled: Note On, Note Off (including Note On with velocity 0), and Control Change 64 (sustain pedal, down at value 64 or more). Input is accepted on any MIDI channel. Release velocity and all other messages (other controllers, program change, pitch bend, clock, active sensing, SysEx) are ignored in v1.

---

## 1A. Keyboard size

Pianos and keyboards come in different sizes. The app must know which keys the user's instrument has.

| ID | Requirement |
|---|---|
| KEY-1 | The user MUST be able to set the number of keys, with presets: **88** (A0–C8, default), **76** (E1–G7), **73** (E1–E7), **61** (C2–C7), **49** (C2–C6), **37** (C3–C6), **25** (C3–C5). |
| KEY-2 | The user MUST be able to set a **custom range** (lowest and highest note), because keyboards with the same number of keys don't always start on the same note. |
| KEY-3 | The app SHOULD offer **range detection**: "press your lowest key, then your highest key" sets the range automatically. |
| KEY-4 | The keyboard range MUST be saved with the device settings (DEV-4), separately for each input device. |
| KEY-5 | The on-screen keyboard MUST draw exactly the configured range. |
| KEY-6 | The random-notes generator MUST NOT produce any note outside the keyboard range. A selected range or preset (section 4.1) is limited to the keyboard range, and the UI shows when that happens. |
| KEY-7 | (v0.8) When a song has notes outside the keyboard range, the session setup MUST warn about it and offer to **shift the octave** of those notes or **skip** them (skipped notes are not scored). |

---

## 1B. Tempo

Tempo is set the same way everywhere (random mode and songs).

| ID | Requirement |
|---|---|
| TMP-1 | The app MUST offer the standard tempo markings as quick presets:<br>Grave 40 · Largo 50 · Adagio 70 · Andante 90 · Moderato 110 · Allegro 130 · Vivace 160 · Presto 180 (BPM). |
| TMP-2 | The app MUST offer a **slider** and a **number field** for any tempo from **20 to 240 BPM** in steps of 1, plus −/+ buttons (±1, and ±5 with Shift). The slider, the field and the presets always stay in sync, and the name of the matching tempo marking (for example "Andante") is shown for the current value. Marking ranges: Grave 20–44, Largo 45–59, Adagio 60–79, Andante 80–99, Moderato 100–119, Allegro 120–144, Vivace 145–169, Presto 170–240 BPM. |
| TMP-3 | The tempo MUST be changeable during a session (while paused, and SHOULD also work while playing). The change takes effect from the next beat. |
| TMP-4 | The beat unit SHOULD be selectable (quarter note by default; half or dotted-quarter for some time signatures). |
| TMP-5 | (v0.8) For songs, the song's own tempo is the default. The UI MUST show both the absolute BPM and the percentage of the original (for example "84 BPM · 70%"). When the song changes tempo, all its tempos are scaled by the same factor. |
| TMP-6 | The metronome and count-in follow the current tempo. |
| TMP-7 | The last tempo used MUST be remembered for each mode (and, from v0.8, for each song). |

---

## 2. Song library and import

### 2.1 MusicXML as the one song format

| ID | Requirement |
|---|---|
| LIB-1 | Every song in the library MUST be stored as MusicXML. |
| LIB-2 | The app MUST open `.musicxml`, `.xml` (uncompressed MusicXML) and `.mxl` (compressed) files, MusicXML version 3.1 or 4.0. |
| LIB-3 | Imported songs MUST be copied into the library folder (`~/.local/share/piano-player/library/`) together with a small metadata file (title, source file, import date, original format). |
| LIB-4 | The library screen MUST list songs with their title, number of parts, length and source format, and SHOULD allow search and delete. |
| LIB-5 | The supported MusicXML subset (v1) is: parts, staves (1 or 2 per part), measures, notes, rests, chords, ties, voices, time signatures, key signatures, tempo markings, repeats (played straight through in v1), and tuplets. Ornaments, grace notes, dynamics and lyrics are read but ignored. Anything not supported MUST be skipped without crashing and noted in an import report. |

### 2.2 MIDI import (MIDI → MusicXML)

| ID | Requirement |
|---|---|
| MID-1 | The app MUST read MIDI files of type 0 and type 1. |
| MID-2 | Each track/channel pair MUST become one MusicXML **part**. Its name comes from the track name or, failing that, from the General MIDI instrument name of its program change. Channel 10 is marked as percussion and can't be chosen for playing. |
| MID-3 | Note timings MUST be **quantised** (snapped) to a grid the user can set (default: 1/16 notes, with triplets detected optionally) so they can be written as notation. |
| MID-4 | Tempo changes, time signatures and key signatures from the MIDI file MUST be carried over. If the key signature is missing, the app SHOULD estimate it from the notes. |
| MID-5 | **Hand splitting** for piano parts: if two tracks look like left and right hand, they MUST be joined into one part with two staves. Otherwise the notes MUST be split by pitch at a split point the user can set (default: middle C, MIDI note 60). |
| MID-6 | Before saving, the import screen MUST show the parts found (name, instrument, number of notes, range) and let the user include or exclude each one, and change the hand-split settings. |
| MID-7 | The conversion MUST be repeatable: the same file with the same settings always produces the same MusicXML. |

### 2.3 PDF import (v2)

| ID | Requirement |
|---|---|
| PDF-1 | (v2) The app MUST convert PDF → MusicXML using Audiveris. |
| PDF-2 | (v2) The app MUST show the recognised result for review before saving, and MAY offer basic fixes. |

---

## 3. Practice session (all song modes)

### 3.1 Setup

Before starting, the user chooses:
- **Part** (instrument) to play, for songs with more than one part.
- **Hands**: right, left, or both (for parts with two staves).
- **Follow mode**: Wait, Tempo, Listen or Play-along.
- **Tempo**: presets, slider or number field (section 1B), defaulting to the song's tempo.
- **Accompaniment**: play the parts you aren't playing (on/off, plus volume), and play the hand you aren't playing (on/off).
- **Section**: a range of measures to play (default: the whole song), and optionally **loop** that section.
- **Count-in**: off, 1 bar or 2 bars, with a metronome on or off.

### 3.2 Follow modes

**Wait mode**
- The music stops at the next expected note or chord (the "target").
- The target counts as **correct** once all of its pitches are held at the same time, or were pressed within the chord window (default 300 ms between the first and last key).
- A key that isn't part of the target counts as **wrong**. It is shown in red, the music doesn't move on, and it adds to the wrong-key count.
- Pressing a key again that was already counted for the target is ignored.
- Accompaniment waits along with the music.

**Tempo mode**
- The music moves at the set tempo. Each expected note has a timing window around its start time:

| Grade | Default window |
|---|---|
| Perfect | ±50 ms |
| Good | ±100 ms |
| OK | ±150 ms |
| Miss | no matching press within ±150 ms |

- A press that doesn't match any expected note within the window counts as **wrong**.
- Each expected note can be matched by at most one press. Each press matches at most one note: the nearest one in time with the same pitch.
- Note length (how long a key is held) is NOT scored in v1.

**Listen mode**
- The app plays the selected part(s) at the set tempo and highlights each note as it plays. Nothing is scored.

**Play-along mode**
- Same timing as Tempo mode. The keys you press are shown, but nothing is scored or graded.

### 3.3 Feedback (shown live)

- Correct notes are marked green, wrong ones red, missed ones grey.
- In Tempo mode, the timing grade for the last note is shown briefly.
- The on-screen keyboard lights up the keys you press, and shows the target keys as a hint (hints can be turned on or off).
- A progress indicator shows the current measure out of the total.

### 3.4 Results (at the end of a session)

- Accuracy % (correct ÷ expected), number of wrong keys, time taken (Wait mode) or grade counts (Tempo mode).
- (v1) The measures with the most mistakes, to show which sections need work.
- (v1) Results are saved per song so progress can be compared over time.

---

## 4. Random notes mode

### 4.1 Settings

| Setting | Values | Default |
|---|---|---|
| Hands | right / left / both | right |
| Range | lowest–highest note, or a preset (for example "C4–C5", "treble staff", "bass staff", "grand staff"); always limited to the keyboard range (KEY-6) | treble staff |
| Key | any major or minor key signature with up to 7 sharps or flats; only notes in that key (minor keys use the natural minor scale), or allow sharps and flats | C major, no sharps or flats |
| Tempo | presets / slider / number field (section 1B) | Andante 90 |
| Note value | how long each generated note or chord lasts: whole, half, quarter, eighth (or a mix, from level 3 up) | quarter |
| Time signature | 2/4, 3/4, 4/4, 6/8 (used for bar lines and the metronome; 6/8 is counted in two dotted-quarter beats) | 4/4 |
| Complexity | level 1–5 (see below) | 1 |
| Follow mode | Wait / Tempo | Wait |
| Length | number of notes, a time limit, or endless | 50 notes |
| **Chord practice** | on/off, plus chord types and inversions | off |

### 4.2 Complexity levels

| Level | What gets generated |
|---|---|
| 1 | Single notes, steps of a 2nd or 3rd, small range |
| 2 | Single notes, jumps up to an octave |
| 3 | Some 2-note intervals (two notes together) |
| 4 | Intervals and 3-note chords, accidentals (sharps/flats) if allowed |
| 5 | Both hands moving independently, any chord from the chosen chord types |

Level details:
- Level 1: the first note is anywhere in a window of 8 notes of the key (an octave). For the right hand the window starts at the bottom of the range; for the left hand it ends at the top. Each next note is a 2nd or 3rd away (one or two notes of the key), never the same note again.
- Level 2: any note in the range, up to an octave (12 semitones) away from the previous one, never the same note again.
- Levels 1–4 with both hands: the hands take turns, one measure each, starting with the right hand. The right hand plays middle C (C4) and above, the left hand below it. With one hand, that hand uses the whole range.
- The chosen note value must fill a measure exactly (for example half notes don't fit 3/4); the setup only offers values that fit.
- The range must hold at least two notes of the key for each hand being practised.

### 4.3 Chord practice

- Chord types to choose from: major, minor, diminished, augmented, major 7, minor 7, dominant 7 (and sus2/sus4 for v1).
- Inversions: root position, 1st, 2nd (3rd for 7th chords). Any combination can be selected.
- The chord name (for example "Dm/F") can be shown or hidden. Hiding it makes it a reading exercise.
- Scored with the Wait or Tempo chord rules from section 3.2.

### 4.4 Generator rules

- If a seed is given, generation MUST be repeatable (the same seed produces the same notes), for tests and for replaying an exercise.
- The generator produces notes a little ahead of time (at least 4 measures of look-ahead), so endless sessions never run out.
- A session MAY be exported as MusicXML.

---

## 5. User interface

### 5.1 Screens

1. **Home**: choose a mode (Song library / Random notes / Devices / Settings).
2. **Devices**: choose input and output, test the connection with live activity, set the keyboard size or range (with range detection), set the latency offset.
3. **Library**: song list, import MusicXML/MIDI (PDF in v2), open a song.
4. **MIDI import**: list of parts, include/exclude, quantisation and hand-split settings, preview.
5. **Session setup**: part, hands, follow mode, tempo, accompaniment, section/loop (section 3.1).
6. **Practice**: the note display, the on-screen keyboard, feedback, and controls (play/pause, restart, tempo, loop).
7. **Results**.

### 5.2 Note display

- **v0.5 (proof of concept):** a simple display, with no proper staff notation. For example: notes as blocks on a timeline lane (piano-roll style) or labelled with note names, split into right-hand and left-hand lanes, with a "now" line and colour feedback. Simple drawing is fine.
- **v1:** proper grand-staff notation (treble + bass, clefs, key and time signatures, bar lines, note heads, stems, rests, ledger lines, accidentals), drawn with HTML5 canvas/SVG. The current position is highlighted and the music scrolls or turns pages automatically.

### 5.3 On-screen keyboard

- Draws the keyboard range from section 1A (88 keys by default).
- Shows: keys being pressed, target keys (hint), and correct/wrong colours.

---

## 6. Non-functional requirements

| ID | Requirement |
|---|---|
| NF-1 | **Latency:** from a key press arriving in the backend to the feedback appearing in the UI should take under 20 ms in normal conditions. Scoring itself does not depend on UI delay (DEV-8). |
| NF-2 | **Platform:** Linux x86-64 (any modern distribution with ALSA). |
| NF-3 | **Packaging:** one installable package (AppImage and/or .deb) that includes the Java runtime and the UI shell. No separate installs needed. |
| NF-4 | **Offline:** works fully offline. The backend only listens on `127.0.0.1` (this computer). |
| NF-5 | **Startup:** the main window appears within 3 s on a normal laptop. |
| NF-6 | **Robustness:** a broken or unsupported file must never crash the app. It produces an error or import report instead. |
| NF-7 | **Storage:** settings in `~/.config/piano-player/`, and the library and progress in `~/.local/share/piano-player/`. |
| NF-8 | **Testability:** the engine, importers and generator have automated tests that run without a MIDI device. |
