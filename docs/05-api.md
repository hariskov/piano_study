# Piano Player: backend API reference

The UI's window shell (`frontend/shell/piano-player.js`) starts `pp api` (optionally `--settings <file>`, default `~/.config/piano-player/settings.json`, and `--library <dir>`, default `~/.local/share/piano-player/musicFiles`) and exchanges JSON-RPC 2.0 messages with it, one per line, over the process's stdin (requests) and stdout (responses and events). See architecture §7 for the design.

Optional fields can be left out. Fields without a value are left out of events too.

## Errors

| Code | Meaning |
|---|---|
| -32700 | The line isn't JSON |
| -32600 | Not a JSON-RPC request (no `method`) |
| -32601 | Unknown method |
| -32602 | Invalid params: wrong JSON shape, or a value that can't be used. `message` says why, for example "Tempo out of range 20-240 BPM: 500" |
| -32603 | Internal error |
| -32001 | Session error, for example "A session is already running; stop it first" or "No session is running" |
| -32002 | Device error, for example "MIDI device unavailable: jdk:Missing" |
| -32003 | File error: a song file can't be read (missing, broken, not MIDI or MusicXML inside); `message` names the file and the reason |
| -32004 | Not found: no song with that id |

## Methods

### `devices.list`
No params. Result: `{"inputs":[{"id","name","description"}],"outputs":[…]}`.

### `devices.rescan`
No params. Looks for devices (there is no automatic background check) and sends `devices.changed` if the list changed. A running session reacts as usual (pauses on a lost input, reconnects a returned one). Result: the device list, as in `devices.list`.

### `devices.monitor`
Params `{"input":"jdk:Piano [hw:3,0,0]"}`. Opens the input outside a session and sends an `input` event for every key and pedal, for the device screen's live keyboard (DEV-6). A second call switches to the new input. Starting a session stops monitoring. Result `null`; -32002 if the device is unavailable.

### `devices.stopMonitor`
No params. Stops monitoring. Result `null`.

### `keyboard.press`, `keyboard.release`
Params `{"pitch":"C4","velocity":80}` (`velocity` is optional and defaults to 80; release ignores it). Plays the built-in virtual keyboard, the input `virtual:keyboard`, which `devices.list` always lists. The UI's on-screen keyboard and computer keys use it to practise without a piano (DEV-7). Result `null`; -32602 for an unknown pitch.

### `library.list`
No params. Result: `{"songs":[{"id","title","fileName","midiFile","partCount","measureCount","sourceFile","format","importedAt","warnings"}]}`, sorted by title. `midiFile` is only there for MIDI imports.

### `library.import`
Params: `{"path":"/home/me/song.mid"}` plus, for MIDI files, optional `grid` (4, 8, 16 or 32, default 16), `triplets` (default false), `splitPoint` (lowest right-hand note, default `"C4"`) and `exclude` (ids of parts to leave out, as `library.preview` gives them; leaving out every part gives -32602). MIDI (`.mid`, `.midi`) is converted and kept with its original file; MusicXML (`.musicxml`, `.xml`, `.mxl`) is saved as it is. Result: the new song, as in `library.list`, with its import warnings. Other file types give -32602; unreadable files -32003.

### `library.preview`
Converts a MIDI file with the same params as `library.import` (`path`, `grid`, `triplets`, `splitPoint`, `exclude`) without saving anything, for the import screen. Result: `{"title","measureCount","timeSignature":"4/4","key":"C major","tempo":120,"parts":[{"id":"P1","name":"Piano","instrument":"Acoustic Grand Piano","staffCount":2,"noteCount":120,"lowest":"A2","highest":"E5"}],"warnings"}`. The conversion is repeatable (MID-7), so these part ids are the ones `exclude` takes. `instrument` is the General MIDI instrument of the part's program change (Acoustic Grand Piano without one). Percussion is never a part. Other file types give -32602; unreadable files -32003.

### `library.open`
Params `{"id":"simple-melody"}`. Result: `{"id","title","composer","file","midiFile","measureCount","timeSignature":"4/4","key":"C major","tempo":120,"parts":[{"id","name","staffCount","noteCount","lowest","highest"}],"warnings"}`. `file` and `midiFile` are full paths. -32004 if there is no such song.

### `library.musicXml`

Params: `{"id":"simple-melody"}`. Result: `{"xml":"<?xml …"}`, the song's saved MusicXML, for drawing the score. It is always plain text: a MusicXML import keeps the original score (unzipped from `.mxl`), and a MIDI import is written by `MusicXmlWriter`. -32004 if there is no such song, -32003 if the file can't be read.

### `progress.history`

Params: `{"songId":"sonatina"}` or `{"exercise":"random"}` (or `"chords"`). Result: `{"sessions":[…]}`, oldest first. Each session: `finishedAt` (ISO time), `exercise`, `songId`, `part`, `hands`, `mode`, `tempo`, `tempoPercent`, `fromBar`, `toBar`, `accuracyPercent`, `timed`, `completedTargets`, `wrongKeys`, `perfect`, `good`, `ok`, `missed`, `elapsedMs`, `passes` and `mistakesByBar` (bar number → wrong keys plus missed notes, songs only). A session is saved when it finishes in Wait or Tempo mode with at least one target done or missed; Listen and Play along are not saved. -32602 without a `songId` or `exercise`.

### `library.delete`
Params `{"id":"simple-melody"}`. Removes the song, its MusicXML and its MIDI file. Result `null`; -32004 if there is no such song.

### `settings.get`
No params. Result: `{"input":"jdk:Piano","output":"jdk:Gervill","keyboards":{"jdk:Piano":"A0-C8"},"tempos":{"wait":72,"tempo":100},"latencyOffsetMs":0,"instrument":0,"keySound":false}`. `instrument` is the General MIDI program (0–127, 0 the grand piano) the keys you play sound with, and `keySound` whether they are played through the output (DEV-13/14). Fields that were never set are left out.

### `settings.update`
Params: any of `input`, `output`, `keyboards` (device id → preset size or range), `tempos` (`"wait"`/`"tempo"` → BPM), `latencyOffsetMs` (−500…500), `instrument` (0–127), `keySound`. Only the given fields change, and the result is saved straight away. Result: the updated settings, as in `settings.get`. A value that can't be used gives -32602, and nothing is saved.

### `sound.test`
Params: optional `instrument` (0–127; default the saved one). Plays a short phrase (C4 E4 G4 C5, then the chord) in that instrument on the saved output, on channel 1, and returns `null` straight away while it plays. -32002 when no output is saved or it can't be opened.

Your own keys: when `keySound` is on and an output is saved, the keys of the monitored input (`devices.monitor`) and of a running session are played through that output on channel 1, after a program change to the saved `instrument`. Playback uses channel 2 and the metronome channel 10.

### `session.start`
Starts a random-notes or chord session. Only one session runs at a time. Leaving out `tempo` uses the saved tempo for the mode, and leaving out `keyboard` uses the saved keyboard of the input device. A started session saves its input and its tempo for its mode (DEV-4, TMP-7). Key press times are moved earlier by the saved latency offset before scoring (DEV-9).

| Param | Default | Notes |
|---|---|---|
| `input` | required | input device id from `devices.list` |
| `exercise` | required | `"random"`, `"chords"` or `"song"` |
| `mode` | `"wait"` | `"wait"`, `"tempo"`, `"listen"` or `"play-along"`. The last two are meant for songs: nothing is scored and the song moves on by itself |
| `tempo` | saved for the mode, else 90 | BPM, 20–240 |
| `key` | `"C"` | e.g. `"Bb"`, `"F#m"` |
| `hands` | `"right"` | `"right"`, `"left"`, `"both"` |
| `range` | by hands | `"C4-C6"`; default C4–C6 right, C2–C4 left, C2–C6 both |
| `keyboard` | saved for the input, else `"88"` | preset size (88, 76, 73, 61, 49, 37, 25) or a range like `"F1-F7"` |
| `noteValue` | `"quarter"` | `"whole"`, `"half"`, `"quarter"`, `"eighth"` |
| `time` | `"4/4"` | time signature |
| `notes` | 50 | number of targets, 0 = endless |
| `countIn` | 1 | bars: 0, 1, 2 |
| `seed` | random | the same seed replays the same exercise |
| `metronome` | none | output device id to play metronome clicks on (`beat` events are sent either way) |
| `level` | 1 | random: 1–5 |
| `accidentals` | false | random: sharps and flats (level 4+) |
| `mixed` | false | random: mixed note values (level 3+) |
| `chordTypes` | `["maj","min"]` | chords: `maj`, `min`, `dim`, `aug`, `maj7`, `min7`, `dom7` |
| `inversions` | `[0]` | chords: 0–3 |
| `songId` | required for songs | song id from `library.list` |
| `part` | first part | songs: part id from `library.open` |
| `tempoPercent` | 100 | songs: the song's own tempos, scaled (its tempo changes stay in proportion) |
| `outOfRange` | `"shift"` | songs: notes outside the keyboard are moved by octaves (`"shift"`) or left out (`"skip"`) |
| `playback` | none | songs: output id that plays the other hand and other parts (the whole song in `listen`). Can be the same output as `metronome` |
| `fromBar`, `toBar` | the whole song | songs: the section to play, bars counted from 1 (a pickup is bar 1) |
| `repeats` | 1 | songs: play the section this many times in a row (1–99) |

Result: `{"seed":42,"tempo":90,"songTempo":100,"notices":[]}`. For songs, `songTempo` is the song's starting tempo, `sectionStart` is where the chosen section starts in the song and `passLength` how long one pass is (both in whole notes, like target starts: a target at `start` is at `sectionStart + start mod passLength` in the score), so the UI can show "84 BPM · 84%", and `notices` says how many notes were moved or skipped. Songs use their own time signature for the count-in and metronome, ignore `tempo`, `key`, `range`, `noteValue`, `time`, `notes` and the random/chord fields, and don't change the saved tempo for the mode. `session.adjust` is refused for songs.

### `session.pause`, `session.resume`, `session.stop`
No params. Result `null`. Error -32001 if no session is running. Pause and resume are ignored in states where they don't apply. Stop finishes the session and sends `session.summary`.

### `session.adjust`
Params: any of `hands`, `level`, `accidentals`, `mixed` (random notes) and `chordTypes`, `inversions` (chords), with the same values as `session.start`. Changes the running exercise: the target being played stays, and from the next measure on the targets follow the new settings (the look-ahead is generated again). A value that can't be played gives -32602, and the session keeps its old settings. Result `null`; -32001 if no session is running.

### `session.changeTempo`
Params `{"bpm":80}`. Takes effect from the next beat (TMP-3) and is saved as the tempo for the session's mode. Result `null`.

## Events

| Method | Params | When |
|---|---|---|
| `session.state` | `{"state":"COUNT_IN" \| "RUNNING" \| "PAUSED" \| "FINISHED"}` | state changes |
| `session.target` | `{"start":"1/4","name":"Dm/F","notes":[{"pitch":"F4","midi":65,"hand":"RIGHT"}]}` | the next note or chord to play (`name` for chords only) |
| `eval` | `{"result":"CORRECT" \| "WRONG" \| "TARGET_COMPLETED" \| "TIMED" \| "MISSED","pitch":"C4","grade":"PERFECT" \| "GOOD" \| "OK","offsetMs":-37}` | after each press (`TIMED` has grade and offset, negative = early), and when a target is complete or a note is missed |
| `input` | `{"type":"NOTE_ON" \| "NOTE_OFF" \| "PEDAL","pitch":"C4","midi":60,"velocity":72,"down":true,"t":123456789}` | every key and pedal event, for the on-screen keyboard (`t` in backend nanoseconds) |
| `beat` | `{"accent":true}` | every counted beat, from the count-in on; `accent` on the first beat of a bar |
| `session.inputLost` | `{"input":"jdk:Piano [hw:3,0,0]"}` | a rescan found the session's input gone; the session is paused |
| `session.inputRestored` | `{"input":"jdk:Piano [hw:3,0,0]"}` | the input is back and reconnected; the session stays paused until `session.resume` |
| `devices.changed` | same as the `devices.list` result | a rescan found a device plugged in or unplugged |
| `session.summary` | `{"timed":false,"completedTargets":50,"correctNotes":60,"wrongKeys":3,"perfect":0,"good":0,"ok":0,"missed":0,"accuracyPercent":95,"elapsedMs":83000,"passes":[]}` | once, after the session finishes. `passes` is the accuracy of each pass when a section was repeated, empty otherwise |

## Example

```
→ {"jsonrpc":"2.0","id":1,"method":"session.start","params":{"input":"jdk:Piano [hw:3,0,0]","exercise":"random","level":2}}
← {"jsonrpc":"2.0","id":1,"result":{"seed":6978407653612318625,"tempo":90}}
← {"jsonrpc":"2.0","method":"session.target","params":{"start":"0","notes":[{"pitch":"E4","midi":64,"hand":"RIGHT"}]}}
← {"jsonrpc":"2.0","method":"session.state","params":{"state":"COUNT_IN"}}
← {"jsonrpc":"2.0","method":"beat","params":{"accent":true}}
← {"jsonrpc":"2.0","method":"session.state","params":{"state":"RUNNING"}}
← {"jsonrpc":"2.0","method":"input","params":{"type":"NOTE_ON","pitch":"E4","midi":64,"velocity":70,"t":912345678}}
← {"jsonrpc":"2.0","method":"eval","params":{"result":"CORRECT","pitch":"E4"}}
← {"jsonrpc":"2.0","method":"eval","params":{"result":"TARGET_COMPLETED"}}
```
