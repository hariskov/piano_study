import { describe, expect, it } from "vitest";
import { followTo, scorePosition, type StepCursor } from "./scorePosition";

function cursorOver(stops: number[]): StepCursor & { index: number; resets: number } {
  return {
    index: 0,
    resets: 0,
    timestamp() {
      return stops[this.index];
    },
    atEnd() {
      return this.index >= stops.length - 1;
    },
    next() {
      this.index++;
    },
    reset() {
      this.index = 0;
      this.resets++;
    },
  };
}

describe("scorePosition", () => {
  it("is the target's start when the whole song plays once", () => {
    expect(scorePosition("3/4", undefined, undefined)).toBe(0.75);
  });

  it("maps every pass of a repeated section back onto the section", () => {
    expect(scorePosition("0", "2", "1")).toBe(2);
    expect(scorePosition("5/4", "2", "1")).toBe(2.25);
    expect(scorePosition("2", "2", "1")).toBe(2);
  });
});

describe("followTo", () => {
  it("moves forward to the stop at the position", () => {
    const cursor = cursorOver([0, 0.25, 0.5, 1]);

    followTo(cursor, 0.5);

    expect(cursor.index).toBe(2);
  });

  it("starts again from the top when the position is behind the cursor", () => {
    const cursor = cursorOver([0, 0.25, 0.5, 1]);
    followTo(cursor, 1);

    followTo(cursor, 0.25);

    expect(cursor.index).toBe(1);
    expect(cursor.resets).toBe(1);
  });

  it("stops at the end of the score", () => {
    const cursor = cursorOver([0, 0.25]);

    followTo(cursor, 9);

    expect(cursor.index).toBe(1);
  });
});
