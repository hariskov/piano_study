import { describe, expect, it } from "vitest";
import { FASTEST_BPM, SLOWEST_BPM, clampBpm, markingFor, TEMPO_MARKINGS } from "./tempoMarking";

describe("tempo markings", () => {
  it.each([
    [20, "Grave"],
    [44, "Grave"],
    [45, "Largo"],
    [60, "Adagio"],
    [79, "Adagio"],
    [80, "Andante"],
    [100, "Moderato"],
    [120, "Allegro"],
    [145, "Vivace"],
    [170, "Presto"],
    [240, "Presto"],
  ])("names %i BPM %s", (bpm, name) => {
    expect(markingFor(bpm).name).toBe(name);
  });

  it("offers the eight presets of the backend", () => {
    expect(TEMPO_MARKINGS.map((marking) => [marking.name, marking.preset])).toEqual([
      ["Grave", 40],
      ["Largo", 50],
      ["Adagio", 70],
      ["Andante", 90],
      ["Moderato", 110],
      ["Allegro", 130],
      ["Vivace", 160],
      ["Presto", 180],
    ]);
  });

  it("keeps a tempo between 20 and 240 whole beats per minute", () => {
    expect(clampBpm(5)).toBe(SLOWEST_BPM);
    expect(clampBpm(500)).toBe(FASTEST_BPM);
    expect(clampBpm(90.6)).toBe(91);
    expect(clampBpm(Number.NaN)).toBe(SLOWEST_BPM);
  });
});
