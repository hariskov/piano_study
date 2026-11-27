import { midiOf } from "../music/pitch";

export interface KeyRange {
  readonly low: number;
  readonly high: number;
}

export interface KeyboardPreset {
  readonly keys: number;
  readonly lowest: string;
  readonly highest: string;
}

export const KEYBOARD_PRESETS: readonly KeyboardPreset[] = [
  { keys: 88, lowest: "A0", highest: "C8" },
  { keys: 76, lowest: "E1", highest: "G7" },
  { keys: 73, lowest: "E1", highest: "E7" },
  { keys: 61, lowest: "C2", highest: "C7" },
  { keys: 49, lowest: "C2", highest: "C6" },
  { keys: 37, lowest: "C3", highest: "C6" },
  { keys: 25, lowest: "C3", highest: "C5" },
];

const FULL_PIANO: KeyRange = { low: 21, high: 108 };

export function keyboardRange(saved: string | undefined): KeyRange {
  if (saved === undefined) {
    return FULL_PIANO;
  }
  const preset = KEYBOARD_PRESETS.find((candidate) => String(candidate.keys) === saved);
  if (preset) {
    return { low: midiOf(preset.lowest), high: midiOf(preset.highest) };
  }
  const [lowest, highest] = saved.split(/-(?=[A-Ga-g])/);
  return { low: midiOf(lowest), high: midiOf(highest) };
}

export function presetFor(range: KeyRange): KeyboardPreset | undefined {
  return KEYBOARD_PRESETS.find((preset) => midiOf(preset.lowest) === range.low && midiOf(preset.highest) === range.high);
}
