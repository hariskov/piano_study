import { isBlackKey, pitchName } from "../music/pitch";

export const BLACK_KEY_OFFSET = 0.3;
export const BLACK_KEY_WIDTH = 0.6;

export interface KeyPosition {
  readonly midi: number;
  readonly name: string;
  readonly black: boolean;
  readonly x: number;
}

export interface KeyboardLayout {
  readonly keys: readonly KeyPosition[];
  readonly whiteKeyCount: number;
}

export function layoutKeys(low: number, high: number): KeyboardLayout {
  const start = isBlackKey(low) ? BLACK_KEY_OFFSET : 0;
  const keys: KeyPosition[] = [];
  let whites = 0;
  let width = start;
  for (let midi = low; midi <= high; midi++) {
    const black = isBlackKey(midi);
    const x = round(black ? start + whites - BLACK_KEY_OFFSET : start + whites);
    keys.push({ midi, name: pitchName(midi), black, x });
    if (!black) {
      whites++;
    }
    width = Math.max(width, round(x + (black ? BLACK_KEY_WIDTH : 1)));
  }
  return { keys, whiteKeyCount: width };
}

function round(value: number): number {
  return Math.round(value * 10) / 10;
}
