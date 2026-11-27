const NAMES = ["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"];
const NATURALS: Record<string, number> = { C: 0, D: 2, E: 4, F: 5, G: 7, A: 9, B: 11 };
const PITCH = /^([A-Ga-g])([#b]?)(-1|\d)$/;

export function pitchName(midi: number): string {
  return NAMES[midi % 12] + (Math.floor(midi / 12) - 1);
}

export function midiOf(name: string): number {
  const match = PITCH.exec(name.trim());
  if (!match) {
    throw new Error(`Not a pitch: ${name}`);
  }
  const accidental = match[2] === "#" ? 1 : match[2] === "b" ? -1 : 0;
  return (Number(match[3]) + 1) * 12 + NATURALS[match[1].toUpperCase()] + accidental;
}

export function isBlackKey(midi: number): boolean {
  return NAMES[midi % 12].length > 1;
}
