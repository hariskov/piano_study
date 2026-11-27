# Piano Study

Desktop piano learning app for Linux: connect a digital piano over USB MIDI, practise random notes, chords and songs, and get instant feedback on every key press.

See [`docs/`](docs) for the vision, specification, architecture and development phases.

## Build

Requires JDK 25. Maven is provided by the wrapper.

```
./mvnw verify
```

The frontend (`frontend/`, React + TypeScript + Vite) is built and tested by the same `./mvnw verify`; Maven downloads its own Node for it.

Run the desktop app:

```
./mvnw -q package -DskipTests
bin/ui
```

The first start downloads the Electron binary (about 100 MB, once). The app starts the backend (`bin/pp api`) itself and stops it when the window closes.

End-to-end smoke test (Playwright driving the real app; needs a display, for example Xvfb on a headless machine):

```
./mvnw -q package -DskipTests
cd frontend && PATH=$PWD/target/node:$PATH npm run e2e        # or: xvfb-run npm run e2e
```

It plays a random-notes session and a chord session on the virtual keyboard and saves screenshots in `frontend/test-results/screens/`.

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
```

For example, `bin/pp random "jdk:Piano [hw:3,0,0]" --level 2 --hands both --mode tempo --tempo 70` prints each note to play and the result of each key press, then a summary. Every session prints its seed, and `--seed` replays the same exercise. Add `--metronome jdk:Gervill` to hear a click on every beat, starting with the count-in.

Try the API by hand: `echo '{"jsonrpc":"2.0","id":1,"method":"devices.list"}' | bin/pp api`.
