# Piano Player: Vision

## The idea

A desktop app for learning piano with a digital piano connected to a Linux laptop over USB MIDI.

The app shows the music for both hands and follows along as you play. Every key you press is checked against the note you should be playing at that moment, and the app shows right away whether it was correct.

## Who it is for

Someone learning piano at home with a digital piano, who wants:
- to learn real songs, one hand or both,
- to practise reading notes,
- immediate feedback on whether the right key was pressed at the right time.

## Core principles

1. **Self-contained desktop app.** It installs and runs like any other app: no browser, no separate Java install, no internet connection needed.
2. **Linux first.** Linux is the only supported system for now. The design should not rule out Windows or macOS later.
3. **MusicXML is the one song format.** Every song is converted to MusicXML when it is imported, and the app only ever opens MusicXML.
   - MIDI files are converted to MusicXML on import (v1).
   - PDF sheet music is converted to MusicXML using optical music recognition (v2).
   - MusicXML files from notation software (MuseScore and others) are opened directly.
4. **The backend does the work, the UI shows it.** Device handling, importing, note generation, playback and scoring all live in a Java backend. The UI is a web-technology front end (HTML5 canvas/SVG) inside a desktop window.
5. **It can be tested without a piano.** Every part of the backend can be tested with simulated MIDI input.

## Modes

| Mode | Source | Version |
|---|---|---|
| **Random notes** | Generated notes, with settings for tempo, difficulty, range and hands, plus a chord practice option | **v0.5 (first)** |
| **Song: MusicXML** | Open a MusicXML file (`.musicxml`, `.xml`, `.mxl`) | v0.8 |
| **Song: MIDI** | Import a MIDI file. It is converted to MusicXML, then you pick which instrument (part) to play | v0.8 (basic), v1 (full import screen) |
| **Song: PDF** | Import a PDF of sheet music. It is converted to MusicXML using OMR, and you review the result | v2 |

## How the app follows your playing

All song modes support four ways of following along:

| Follow mode | What happens | Scored |
|---|---|---|
| **Wait** | The music stops at each note or chord until you play it correctly | Yes (correct / wrong keys) |
| **Tempo** | The music keeps moving at the set tempo, and each press is judged on pitch and timing | Yes (pitch + timing) |
| **Listen** | The app plays the song while the notes are highlighted | No |
| **Play-along** | The music moves at tempo and you play along, with no scoring | No |

Parts you aren't playing (the other hand, other instruments) can be played back by the app.

## Versions

- **v0.5: proof of concept, random notes and chords.** Random notes and chord practice work end to end: piano → backend → evaluation → UI feedback. It includes keyboard size settings, tempo settings and a simple display (no proper staff notation). It is packaged as a self-contained Linux app.
- **v0.8: songs.** Open MusicXML files and import MIDI files (converted to MusicXML), choose a part and hands, and practise in Wait / Tempo / Listen / Play-along mode, still with the simple display.
- **v1.0: first real version.** Proper sheet-music display for both hands, MIDI import with instrument choice, all four follow modes, chord practice, saved progress, Linux packages.
- **v2.0: PDF import.** PDF → MusicXML using Audiveris, with a screen to review and fix the result.

## Out of scope (for now)

- Windows and macOS builds.
- Running in a browser, or online and cloud features.
- Editing sheet music (beyond fixing OMR results in v2).
- Recording or listening to audio from a microphone. Input is MIDI only.
- Teaching content such as lessons and courses.
