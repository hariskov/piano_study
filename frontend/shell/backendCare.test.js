import { describe, expect, it } from "vitest";
import { logFile, shouldRestart } from "./backendCare.js";

describe("shouldRestart", () => {
  it("restarts a backend that stopped unexpectedly", () => {
    expect(shouldRestart([10_000], 10_000)).toBe(true);
  });

  it("gives up after three stops within a minute", () => {
    expect(shouldRestart([1_000, 20_000, 40_000], 40_000)).toBe(true);
    expect(shouldRestart([1_000, 20_000, 40_000, 50_000], 50_000)).toBe(false);
  });

  it("forgets stops older than a minute", () => {
    expect(shouldRestart([1_000, 2_000, 3_000, 70_000], 70_000)).toBe(true);
  });
});

describe("logFile", () => {
  it("lives in the XDG state folder", () => {
    expect(logFile((name) => (name === "XDG_STATE_HOME" ? "/state" : null), "/home/me")).toBe("/state/piano-player/backend.log");
  });

  it("falls back to ~/.local/state", () => {
    expect(logFile(() => null, "/home/me")).toBe("/home/me/.local/state/piano-player/backend.log");
  });
});
