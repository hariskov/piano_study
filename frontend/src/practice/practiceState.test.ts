import { describe, expect, it } from "vitest";
import { initialPractice, practiceReducer, type PracticeState } from "./practiceState";
import type { TargetEvent } from "../api/protocol";

const C4: TargetEvent = { start: "0", notes: [{ pitch: "C4", midi: 60, hand: "RIGHT" }] };
const CHORD: TargetEvent = {
  start: "1/4",
  name: "C",
  notes: [
    { pitch: "C4", midi: 60, hand: "RIGHT" },
    { pitch: "E4", midi: 64, hand: "RIGHT" },
    { pitch: "G4", midi: 67, hand: "RIGHT" },
  ],
};

function apply(...actions: Parameters<typeof practiceReducer>[1][]): PracticeState {
  return actions.reduce(practiceReducer, initialPractice);
}

describe("practice state", () => {
  it("follows the session state", () => {
    expect(apply({ event: "session.state", params: { state: "COUNT_IN" } }).state).toBe("COUNT_IN");
  });

  it("shows the current target", () => {
    expect(apply({ event: "session.target", params: C4 }).target).toEqual(C4);
  });

  it("moves a completed target into the history as correct", () => {
    const state = apply(
      { event: "session.target", params: C4 },
      { event: "eval", params: { result: "CORRECT", pitch: "C4" } },
      { event: "session.target", params: CHORD },
    );

    expect(state.history).toEqual([{ target: C4, result: "correct" }]);
    expect(state.target).toEqual(CHORD);
  });

  it("marks a target with any missed note as missed", () => {
    const state = apply(
      { event: "session.target", params: CHORD },
      { event: "eval", params: { result: "TIMED", pitch: "C4", grade: "GOOD", offsetMs: 40 } },
      { event: "eval", params: { result: "MISSED", pitch: "E4" } },
      { event: "session.target", params: C4 },
    );

    expect(state.history[0].result).toBe("missed");
  });

  it("keeps the last timing grade", () => {
    const state = apply({ event: "eval", params: { result: "TIMED", pitch: "C4", grade: "PERFECT", offsetMs: -12 } });

    expect(state.lastGrade).toEqual({ grade: "PERFECT", offsetMs: -12 });
  });

  it("colours keys while they are held, by their result", () => {
    const pressed = apply(
      { event: "input", params: { type: "NOTE_ON", pitch: "C4", midi: 60, velocity: 70, t: 1 } },
      { event: "eval", params: { result: "CORRECT", pitch: "C4" } },
      { event: "input", params: { type: "NOTE_ON", pitch: "D4", midi: 62, velocity: 70, t: 2 } },
      { event: "eval", params: { result: "WRONG", pitch: "D4" } },
    );

    expect([...pressed.pressed]).toEqual([60, 62]);
    expect(pressed.marks.get(60)).toBe("correct");
    expect(pressed.marks.get(62)).toBe("wrong");
    expect(pressed.wrongKeys).toBe(1);

    const released = practiceReducer(pressed, {
      event: "input",
      params: { type: "NOTE_OFF", pitch: "D4", midi: 62, t: 3 },
    });
    expect(released.pressed.has(62)).toBe(false);
    expect(released.marks.has(62)).toBe(false);
  });

  it("counts beats and remembers accents", () => {
    const state = apply({ event: "beat", params: { accent: true } }, { event: "beat", params: { accent: false } });

    expect(state.beat).toEqual({ count: 2, accent: false });
  });

  it("knows when the input is lost and back", () => {
    const lost = apply({ event: "session.inputLost", params: { input: "jdk:Piano" } });
    expect(lost.inputLost).toBe(true);

    expect(practiceReducer(lost, { event: "session.inputRestored", params: { input: "jdk:Piano" } }).inputLost).toBe(false);
  });

  it("keeps the summary and finishes the last target", () => {
    const summary = {
      timed: false, completedTargets: 1, correctNotes: 1, wrongKeys: 0, perfect: 0, good: 0, ok: 0, missed: 0,
      accuracyPercent: 100, elapsedMs: 4000,
    };
    const state = apply(
      { event: "session.target", params: C4 },
      { event: "eval", params: { result: "CORRECT", pitch: "C4" } },
      { event: "session.summary", params: summary },
    );

    expect(state.summary).toEqual(summary);
    expect(state.history).toHaveLength(1);
    expect(state.target).toBeUndefined();
  });

  it("keeps only the most recent history", () => {
    let state = initialPractice;
    for (let i = 0; i < 30; i++) {
      state = practiceReducer(state, { event: "session.target", params: { ...C4, start: String(i) } });
    }

    expect(state.history).toHaveLength(12);
    expect(state.history[11].target.start).toBe("28");
  });
});
