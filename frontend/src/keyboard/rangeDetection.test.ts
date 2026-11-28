import { describe, expect, it } from "vitest";
import { START_DETECTION, detectStep } from "./rangeDetection";

describe("detectStep", () => {
  it("takes the first key, then the second, as the range in either order", () => {
    const afterFirst = detectStep(START_DETECTION, 108);
    expect(afterFirst).toEqual({ step: "highest", first: 108 });
    expect(detectStep(afterFirst, 21)).toEqual({ step: "done", range: "A0-C8" });
  });

  it("waits for a different key when the first one is pressed again", () => {
    const afterFirst = detectStep(START_DETECTION, 36);
    expect(detectStep(afterFirst, 36)).toBe(afterFirst);
  });

  it("names sharps the way the backend reads them", () => {
    expect(detectStep(detectStep(START_DETECTION, 30), 97)).toEqual({ step: "done", range: "F#1-C#7" });
  });
});
