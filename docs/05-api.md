# Piano Player: backend API reference

The UI's Electron main process starts `pp api` and exchanges JSON-RPC 2.0 messages with it, one per line, over the process's stdin (requests) and stdout (responses and events). See architecture §7 for the design.

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

## Methods

### `devices.list`
No params. Result: `{"inputs":[{"id","name","description"}],"outputs":[…]}`.

### `session.start`
Starts a random-notes or chord session. Only one session runs at a time.

| Param | Default | Notes |
|---|---|---|
| `input` | required | input device id from `devices.list` |
| `exercise` | required | `"random"` or `"chords"` |
| `mode` | `"wait"` | `"wait"` or `"tempo"` |
| `tempo` | 90 | BPM, 20–240 |
| `key` | `"C"` | e.g. `"Bb"`, `"F#m"` |
| `hands` | `"right"` | `"right"`, `"left"`, `"both"` |
| `range` | by hands | `"C4-C6"`; default C4–C6 right, C2–C4 left, C2–C6 both |
| `keyboard` | `"88"` | preset size (88, 76, 73, 61, 49, 37, 25) or a range like `"F1-F7"` |
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

Result: `{"seed":42}`.

### `session.pause`, `session.resume`, `session.stop`
No params. Result `null`. Error -32001 if no session is running. Pause and resume are ignored in states where they don't apply. Stop finishes the session and sends `session.summary`.

### `session.changeTempo`
Params `{"bpm":80}`. Takes effect from the next beat (TMP-3). Result `null`.

## Events

| Method | Params | When |
|---|---|---|
| `session.state` | `{"state":"COUNT_IN" \| "RUNNING" \| "PAUSED" \| "FINISHED"}` | state changes |
| `session.target` | `{"start":"1/4","name":"Dm/F","notes":[{"pitch":"F4","midi":65,"hand":"RIGHT"}]}` | the next note or chord to play (`name` for chords only) |
| `eval` | `{"result":"CORRECT" \| "WRONG" \| "TARGET_COMPLETED" \| "TIMED" \| "MISSED","pitch":"C4","grade":"PERFECT" \| "GOOD" \| "OK","offsetMs":-37}` | after each press (`TIMED` has grade and offset, negative = early), and when a target is complete or a note is missed |
| `input` | `{"type":"NOTE_ON" \| "NOTE_OFF" \| "PEDAL","pitch":"C4","midi":60,"velocity":72,"down":true,"t":123456789}` | every key and pedal event, for the on-screen keyboard (`t` in backend nanoseconds) |
| `beat` | `{"accent":true}` | every counted beat, from the count-in on; `accent` on the first beat of a bar |
| `session.summary` | `{"timed":false,"completedTargets":50,"correctNotes":60,"wrongKeys":3,"perfect":0,"good":0,"ok":0,"missed":0,"accuracyPercent":95,"elapsedMs":83000}` | once, after the session finishes |

## Example

```
→ {"jsonrpc":"2.0","id":1,"method":"session.start","params":{"input":"jdk:Piano [hw:3,0,0]","exercise":"random","level":2}}
← {"jsonrpc":"2.0","id":1,"result":{"seed":6978407653612318625}}
← {"jsonrpc":"2.0","method":"session.target","params":{"start":"0","notes":[{"pitch":"E4","midi":64,"hand":"RIGHT"}]}}
← {"jsonrpc":"2.0","method":"session.state","params":{"state":"COUNT_IN"}}
← {"jsonrpc":"2.0","method":"beat","params":{"accent":true}}
← {"jsonrpc":"2.0","method":"session.state","params":{"state":"RUNNING"}}
← {"jsonrpc":"2.0","method":"input","params":{"type":"NOTE_ON","pitch":"E4","midi":64,"velocity":70,"t":912345678}}
← {"jsonrpc":"2.0","method":"eval","params":{"result":"CORRECT","pitch":"E4"}}
← {"jsonrpc":"2.0","method":"eval","params":{"result":"TARGET_COMPLETED"}}
```
