export const SLOWEST_BPM = 20;
export const FASTEST_BPM = 240;

export interface TempoMarking {
  readonly name: string;
  readonly preset: number;
  readonly lowest: number;
}

export const TEMPO_MARKINGS: readonly TempoMarking[] = [
  { name: "Grave", preset: 40, lowest: 20 },
  { name: "Largo", preset: 50, lowest: 45 },
  { name: "Adagio", preset: 70, lowest: 60 },
  { name: "Andante", preset: 90, lowest: 80 },
  { name: "Moderato", preset: 110, lowest: 100 },
  { name: "Allegro", preset: 130, lowest: 120 },
  { name: "Vivace", preset: 160, lowest: 145 },
  { name: "Presto", preset: 180, lowest: 170 },
];

export function markingFor(bpm: number): TempoMarking {
  return TEMPO_MARKINGS.findLast((marking) => marking.lowest <= bpm) ?? TEMPO_MARKINGS[0];
}

export function clampBpm(bpm: number): number {
  if (Number.isNaN(bpm)) {
    return SLOWEST_BPM;
  }
  return Math.min(FASTEST_BPM, Math.max(SLOWEST_BPM, Math.round(bpm)));
}
