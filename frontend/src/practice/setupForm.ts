import type { ChordLabels } from "./chordLabel";
import type { SessionStartParams, SettingsView } from "../api/protocol";

export type Exercise = "random" | "chords";
export type Mode = "wait" | "tempo";
export type Hands = "right" | "left" | "both";
export type NoteValue = "whole" | "half" | "quarter" | "eighth";

export interface Setup {
  exercise: Exercise;
  mode: Mode;
  tempo: number;
  hands: Hands;
  level: number;
  key: string;
  noteValue: NoteValue;
  time: string;
  notes: number;
  endless: boolean;
  countIn: number;
  metronome: boolean;
  accidentals: boolean;
  mixed: boolean;
  chordTypes: string[];
  inversions: number[];
  chordLabels: ChordLabels;
  range: string;
}

const DEFAULT_TEMPO = 90;

export function savedTempo(settings: SettingsView, mode: Mode): number {
  return settings.tempos[mode] ?? DEFAULT_TEMPO;
}

export function defaultSetup(settings: SettingsView): Setup {
  return {
    exercise: "random",
    mode: "wait",
    tempo: savedTempo(settings, "wait"),
    hands: "right",
    level: 1,
    key: "C",
    noteValue: "quarter",
    time: "4/4",
    notes: 50,
    endless: false,
    countIn: 1,
    metronome: true,
    accidentals: false,
    mixed: false,
    chordTypes: ["maj", "min"],
    inversions: [0],
    chordLabels: "both",
    range: "",
  };
}

export function toStartParams(setup: Setup, input: string, output: string | undefined): SessionStartParams {
  const params: SessionStartParams = {
    input,
    exercise: setup.exercise,
    mode: setup.mode,
    tempo: setup.tempo,
    key: setup.key,
    hands: setup.hands,
    noteValue: setup.noteValue,
    time: setup.time,
    notes: setup.endless ? 0 : setup.notes,
    countIn: setup.countIn,
  };
  if (setup.metronome && output !== undefined) {
    params.metronome = output;
  }
  if (setup.range.trim() !== "") {
    params.range = setup.range.trim();
  }
  if (setup.exercise === "random") {
    return { ...params, level: setup.level, accidentals: setup.accidentals, mixed: setup.mixed };
  }
  return { ...params, chordTypes: setup.chordTypes, inversions: setup.inversions };
}
