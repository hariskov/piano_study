# Phase 1: manual tests with the real piano

Not run yet. The piano is needed; run these together with the open Phase 0 checks (`pp echo` latency, `pp detect-range`).

Find the input id with `bin/pp devices` (it was `jdk:Piano [hw:3,0,0]` for the P-45) and set the P-45 touch to Medium.

| Check | Command | Result |
|---|---|---|
| 50-note random session, Wait mode, right hand | `bin/pp random <id>` | |
| Wrong keys are reported and the music waits | `bin/pp random <id> --notes 10` | |
| Both hands take turns at level 2 | `bin/pp random <id> --hands both --level 2` | |
| Level 4 intervals and chords feel playable | `bin/pp random <id> --level 4 --mixed` | |
| Level 5 both hands together | `bin/pp random <id> --hands both --level 5 --notes 20` | |
| Tempo mode grades feel fair with the metronome | `bin/pp random <id> --mode tempo --tempo 60 --metronome jdk:Gervill` | |
| Metronome steady at 40, 90 and 240 BPM, accent on each bar, 6/8 in two | `bin/pp random <id> --tempo 40 --metronome jdk:Gervill` (and 90, 240, `--time 6/8`) | |
| Chord session, held and quick (staccato) chords both count | `bin/pp chords <id> --types maj,min --inversions 0,1` | |
| Chord names hidden as a reading exercise | `bin/pp chords <id> --show notes` | |
| `--seed` replays the same exercise | `bin/pp random <id> --seed 42` twice | |
| Ctrl+C stops cleanly and prints the summary | any of the above | |
