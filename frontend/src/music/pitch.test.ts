import { describe, expect, it } from "vitest";
import { isBlackKey, midiOf, pitchName } from "./pitch";

describe("pitch", () => {
  it.each([
    [60, "C4"],
    [61, "C#4"],
    [21, "A0"],
    [108, "C8"],
    [0, "C-1"],
  ])("names MIDI %i as %s", (midi, name) => {
    expect(pitchName(midi)).toBe(name);
    expect(midiOf(name)).toBe(midi);
  });

  it("reads flats too", () => {
    expect(midiOf("Bb3")).toBe(58);
    expect(midiOf("eb4")).toBe(63);
  });

  it("rejects names that are not pitches", () => {
    expect(() => midiOf("H4")).toThrow("Not a pitch: H4");
  });

  it("knows the black keys", () => {
    expect([60, 61, 62, 63, 64, 65, 66].map(isBlackKey)).toEqual([false, true, false, true, false, false, true]);
  });
});
