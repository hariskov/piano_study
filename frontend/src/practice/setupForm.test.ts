import { describe, expect, it } from "vitest";
import { defaultSetup, toStartParams } from "./setupForm";

describe("setup form", () => {
  it("starts from the backend's defaults with the saved tempo", () => {
    const setup = defaultSetup({ keyboards: {}, tempos: { wait: 72 }, latencyOffsetMs: 0, instrument: 0, keySound: false });

    expect(setup).toMatchObject({ exercise: "random", mode: "wait", tempo: 72, hands: "right", level: 1, key: "C" });
  });

  it("uses 90 BPM when nothing is saved", () => {
    expect(defaultSetup({ keyboards: {}, tempos: {}, latencyOffsetMs: 0, instrument: 0, keySound: false }).tempo).toBe(90);
  });

  it("turns a random-notes setup into session.start params", () => {
    const setup = { ...defaultSetup({ keyboards: {}, tempos: {}, latencyOffsetMs: 0, instrument: 0, keySound: false }), level: 4, accidentals: true };

    expect(toStartParams(setup, "jdk:Piano", "jdk:Gervill")).toEqual({
      input: "jdk:Piano",
      exercise: "random",
      mode: "wait",
      tempo: 90,
      key: "C",
      hands: "right",
      noteValue: "quarter",
      time: "4/4",
      notes: 50,
      countIn: 1,
      metronome: "jdk:Gervill",
      level: 4,
      accidentals: true,
      mixed: false,
    });
  });

  it("sends chord settings only for chords and leaves out an empty range", () => {
    const setup = {
      ...defaultSetup({ keyboards: {}, tempos: {}, latencyOffsetMs: 0, instrument: 0, keySound: false }),
      exercise: "chords" as const,
      chordTypes: ["maj", "dom7"],
      inversions: [0, 1],
      range: "  ",
      metronome: false,
      endless: true,
    };

    const params = toStartParams(setup, "virtual:keyboard", "jdk:Gervill");

    expect(params).toMatchObject({ exercise: "chords", chordTypes: ["maj", "dom7"], inversions: [0, 1], notes: 0 });
    expect(params).not.toHaveProperty("level");
    expect(params).not.toHaveProperty("range");
    expect(params).not.toHaveProperty("metronome");
  });

  it("passes a chosen range", () => {
    const setup = { ...defaultSetup({ keyboards: {}, tempos: {}, latencyOffsetMs: 0, instrument: 0, keySound: false }), range: "C4-G5" };

    expect(toStartParams(setup, "x", undefined)).toMatchObject({ range: "C4-G5" });
  });
});
