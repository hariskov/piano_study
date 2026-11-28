import { describe, expect, it } from "vitest";
import { targetLabel } from "./chordLabel";
import type { TargetEvent } from "../api/protocol";

const CHORD: TargetEvent = {
  start: "0",
  name: "Dm/F",
  notes: [
    { pitch: "F4", midi: 65, hand: "RIGHT" },
    { pitch: "A4", midi: 69, hand: "RIGHT" },
    { pitch: "D5", midi: 74, hand: "RIGHT" },
  ],
};

describe("targetLabel", () => {
  it("shows a chord's name, its notes or both", () => {
    expect(targetLabel(CHORD, "name")).toBe("Dm/F");
    expect(targetLabel(CHORD, "notes")).toBe("F4 A4 D5");
    expect(targetLabel(CHORD, "both")).toBe("Dm/F · F4 A4 D5");
  });

  it("shows the notes of anything without a name", () => {
    const note: TargetEvent = { start: "0", notes: [{ pitch: "E4", midi: 64, hand: "RIGHT" }] };
    expect(targetLabel(note, "name")).toBe("E4");
    expect(targetLabel(note, "both")).toBe("E4");
  });
});
