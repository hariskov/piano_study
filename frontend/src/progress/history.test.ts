import { describe, expect, it } from "vitest";
import { previousOf, troubleBars } from "./history";
import type { PastSession } from "../api/protocol";

function session(accuracyPercent: number, mistakesByBar: Record<string, number> = {}): PastSession {
  return {
    finishedAt: "2026-11-28T10:00:00Z",
    exercise: "song",
    songId: "sonatina",
    hands: "right",
    mode: "wait",
    tempo: 90,
    accuracyPercent,
    timed: false,
    completedTargets: 30,
    wrongKeys: 2,
    perfect: 0,
    good: 0,
    ok: 0,
    missed: 0,
    elapsedMs: 40000,
    passes: [],
    mistakesByBar,
  };
}

describe("previousOf", () => {
  it("is the session before the one just saved", () => {
    expect(previousOf([session(60), session(70), session(80)])?.accuracyPercent).toBe(70);
  });

  it("is missing the first time", () => {
    expect(previousOf([session(80)])).toBeUndefined();
    expect(previousOf([])).toBeUndefined();
  });
});

describe("troubleBars", () => {
  it("lists the bars with the most mistakes first, at most three", () => {
    expect(troubleBars(session(80, { "2": 1, "6": 4, "7": 4, "12": 2, "3": 1 }))).toEqual([6, 7, 12]);
  });

  it("is empty without mistakes", () => {
    expect(troubleBars(session(100))).toEqual([]);
  });
});
