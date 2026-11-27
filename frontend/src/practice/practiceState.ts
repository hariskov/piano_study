import type { ApiEvents, SummaryEvent, TargetEvent } from "../api/protocol";
import type { KeyMark } from "../keyboard/PianoKeyboard";
import { midiOf } from "../music/pitch";

export type PracticeAction = { [E in keyof ApiEvents]: { event: E; params: ApiEvents[E] } }[keyof ApiEvents];

export interface PlayedTarget {
  readonly target: TargetEvent;
  readonly result: "correct" | "missed";
}

export interface PracticeState {
  readonly state: string;
  readonly target?: TargetEvent;
  readonly targetMissed: boolean;
  readonly history: readonly PlayedTarget[];
  readonly pressed: ReadonlySet<number>;
  readonly marks: ReadonlyMap<number, KeyMark>;
  readonly wrongKeys: number;
  readonly lastGrade?: { grade: string; offsetMs: number };
  readonly beat: { count: number; accent: boolean };
  readonly inputLost: boolean;
  readonly summary?: SummaryEvent;
}

const HISTORY_LENGTH = 12;

export const initialPractice: PracticeState = {
  state: "STARTING",
  targetMissed: false,
  history: [],
  pressed: new Set(),
  marks: new Map(),
  wrongKeys: 0,
  beat: { count: 0, accent: false },
  inputLost: false,
};

export function practiceReducer(current: PracticeState, action: PracticeAction): PracticeState {
  switch (action.event) {
    case "session.state":
      return { ...current, state: action.params.state };
    case "session.target":
      return { ...finishTarget(current), target: action.params, targetMissed: false };
    case "eval":
      return evaluated(current, action.params);
    case "input": {
      const { midi, type } = action.params;
      if (midi === undefined) {
        return current;
      }
      const pressed = new Set(current.pressed);
      const marks = new Map(current.marks);
      if (type === "NOTE_ON") {
        pressed.add(midi);
      } else {
        pressed.delete(midi);
        marks.delete(midi);
      }
      return { ...current, pressed, marks };
    }
    case "beat":
      return { ...current, beat: { count: current.beat.count + 1, accent: action.params.accent } };
    case "session.inputLost":
      return { ...current, inputLost: true };
    case "session.inputRestored":
      return { ...current, inputLost: false };
    case "session.summary":
      return { ...finishTarget(current), target: undefined, summary: action.params };
    case "devices.changed":
      return current;
  }
}

function evaluated(current: PracticeState, evaluation: ApiEvents["eval"]): PracticeState {
  const midi = evaluation.pitch === undefined ? undefined : midiOf(evaluation.pitch);
  switch (evaluation.result) {
    case "CORRECT":
      return { ...current, marks: marked(current.marks, midi, "correct") };
    case "TIMED":
      return {
        ...current,
        marks: marked(current.marks, midi, "correct"),
        lastGrade: { grade: evaluation.grade ?? "", offsetMs: evaluation.offsetMs ?? 0 },
      };
    case "WRONG":
      return { ...current, marks: marked(current.marks, midi, "wrong"), wrongKeys: current.wrongKeys + 1 };
    case "MISSED":
      return { ...current, targetMissed: true };
    default:
      return current;
  }
}

function marked(marks: ReadonlyMap<number, KeyMark>, midi: number | undefined, mark: KeyMark) {
  if (midi === undefined) {
    return marks;
  }
  return new Map(marks).set(midi, mark);
}

function finishTarget(current: PracticeState): PracticeState {
  if (current.target === undefined) {
    return current;
  }
  const played: PlayedTarget = { target: current.target, result: current.targetMissed ? "missed" : "correct" };
  return { ...current, history: [...current.history, played].slice(-HISTORY_LENGTH) };
}
