# Piano Study

Desktop piano learning app for Linux: connect a digital piano over USB MIDI, practise random notes, chords and songs, and get instant feedback on every key press.

See [`docs/`](docs) for the vision, specification, architecture and development phases.

## Build

Requires JDK 25 and Node.js 24 or newer (on openSUSE: `sudo zypper in nodejs26 npm26`). Maven is provided by the wrapper.

```
./mvnw verify
```

The frontend (`frontend/`, React + TypeScript + Vite) is built and tested by the same `./mvnw verify`, using the system Node.

Run the desktop app:

```
./mvnw -q package -DskipTests
bin/ui
```

The window uses the system's WebKitGTK through GJS (on openSUSE `sudo zypper in gjs typelib-1_0-WebKit-6_0`). The app starts the backend (`bin/pp api`) itself and stops it when the window closes. `PP_DEVTOOLS=1 bin/ui` turns on the WebKit inspector.

Build the app as one executable file (about 130 MB). It needs the JDK's jmods (on openSUSE `sudo zypper in java-25-openjdk-jmods`):

```
./mvnw -Pbundle package -DskipTests
frontend/target/bundle/piano-player-0.1.0-x86_64.sh
```

It is a shell script with the app packed inside. The first start unpacks it (about 340 MB) into `~/.cache/piano-player/app/` and removes older unpacked builds; later starts run it from there. It contains its own Java runtime, so the target machine needs nothing else installed.

End-to-end smoke test. It runs inside the real app window with the real backend and a fresh settings and library folder. It needs a display and the same GJS and WebKitGTK packages as `bin/ui`:

```
./mvnw -q package -DskipTests
cd frontend && npm run e2e
```

It prints one line per test and exits with 0 when all pass. Screenshots of each step go to `frontend/test-results/screens/`.

It plays a random-notes session and a chord session on the virtual keyboard, imports a song into the library, shows its sheet music and practises it twice in a row, and saves screenshots in `frontend/test-results/screens/`.

## Command-line tools

```
./mvnw -q package -DskipTests
bin/pp --help
bin/pp devices          # list MIDI inputs and outputs with their ids
bin/pp monitor <id>     # print key presses, releases and pedal changes (Ctrl+C to stop)
bin/pp echo <in> <out>  # play what you press through an output, e.g. jdk:Gervill
bin/pp detect-range <id> # press the lowest, then the highest key to find the keyboard's range
bin/pp random <id>      # practise random notes; see --help for hands, level, key, tempo, mode
bin/pp chords <id>      # practise chords; --types maj,min,dom7 --inversions 0,1 --show both|name|notes
bin/pp api              # JSON-RPC on stdin/stdout, one message per line (what the UI uses)
bin/pp import <file>    # add a MIDI or MusicXML file to the song library (--out x.musicxml to only convert MIDI)
bin/pp songs            # list the song library; bin/pp songs "<name>" opens one, --delete "<name>" removes it
```

For example, `bin/pp random "jdk:Piano [hw:3,0,0]" --level 2 --hands both --mode tempo --tempo 70` prints each note to play and the result of each key press, then a summary. Every session prints its seed, and `--seed` replays the same exercise. Add `--metronome jdk:Gervill` to hear a click on every beat, starting with the count-in.

Songs are kept in `~/.local/share/piano-player/musicFiles/`: `index.json`, `xml/<Song title>.musicxml`, `midi/` (the original MIDI files) and `sheets/`.

Try the API by hand: `echo '{"jsonrpc":"2.0","id":1,"method":"devices.list"}' | bin/pp api`.
