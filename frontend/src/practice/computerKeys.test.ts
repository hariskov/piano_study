import { describe, expect, it } from "vitest";
import { midiForKey } from "./computerKeys";

describe("computer keys", () => {
  it.each([
    ["a", 60],
    ["w", 61],
    ["s", 62],
    ["e", 63],
    ["d", 64],
    ["f", 65],
    ["t", 66],
    ["k", 72],
    [";", 76],
    ["A", 60],
  ])("plays %s as MIDI %i", (key, midi) => {
    expect(midiForKey(key)).toBe(midi);
  });

  it("ignores keys that are not mapped", () => {
    expect(midiForKey("q")).toBeUndefined();
    expect(midiForKey("Enter")).toBeUndefined();
  });
});
