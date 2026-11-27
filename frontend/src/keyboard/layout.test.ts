import { describe, expect, it } from "vitest";
import { layoutKeys } from "./layout";

describe("keyboard layout", () => {
  it("lays out all 88 keys of a piano", () => {
    const layout = layoutKeys(21, 108);

    expect(layout.keys).toHaveLength(88);
    expect(layout.keys.filter((key) => !key.black)).toHaveLength(52);
    expect(layout.whiteKeyCount).toBe(52);
  });

  it("places white keys side by side and black keys between them", () => {
    const layout = layoutKeys(60, 64);

    expect(layout.keys.map((key) => [key.name, key.black, key.x])).toEqual([
      ["C4", false, 0],
      ["C#4", true, 0.7],
      ["D4", false, 1],
      ["D#4", true, 1.7],
      ["E4", false, 2],
    ]);
  });

  it("starts a range that begins on a black key half a key in", () => {
    const layout = layoutKeys(61, 62);

    expect(layout.keys.map((key) => key.x)).toEqual([0, 0.3]);
    expect(layout.whiteKeyCount).toBe(1.3);
  });
});
