# Piano Study

Desktop piano learning app for Linux: connect a digital piano over USB MIDI, practise random notes, chords and songs, and get instant feedback on every key press.

See [`docs/`](docs) for the vision, specification, architecture and development phases.

## Build

Requires JDK 25. Maven is provided by the wrapper.

```
./mvnw verify
```

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
