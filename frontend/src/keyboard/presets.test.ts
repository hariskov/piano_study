import { describe, expect, it } from "vitest";
import { KEYBOARD_PRESETS, keyboardRange, presetFor } from "./presets";

describe("keyboard presets", () => {
  it("matches the backend presets", () => {
    expect(KEYBOARD_PRESETS.map((preset) => `${preset.keys} ${preset.lowest}-${preset.highest}`)).toEqual([
      "88 A0-C8",
      "76 E1-G7",
      "73 E1-E7",
      "61 C2-C7",
      "49 C2-C6",
      "37 C3-C6",
      "25 C3-C5",
    ]);
  });

  it("reads a saved keyboard as a preset size or a range", () => {
    expect(keyboardRange("61")).toEqual({ low: 36, high: 96 });
    expect(keyboardRange("F1-F7")).toEqual({ low: 29, high: 101 });
    expect(keyboardRange(undefined)).toEqual({ low: 21, high: 108 });
  });

  it("finds the preset of a range", () => {
    expect(presetFor({ low: 36, high: 96 })?.keys).toBe(61);
    expect(presetFor({ low: 29, high: 101 })).toBeUndefined();
  });
});
