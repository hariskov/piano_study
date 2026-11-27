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
```
