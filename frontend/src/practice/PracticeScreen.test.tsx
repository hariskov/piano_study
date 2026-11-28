import { describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { PracticeScreen } from "./PracticeScreen";
import { BackendContext } from "../app/backendContext";
import { FakeBackend } from "../testing/fakeBackend";
import type { SessionStartParams } from "../api/protocol";
import { defaultSetup, type Setup } from "./setupForm";

const PARAMS: SessionStartParams = { input: "virtual:keyboard", exercise: "random", mode: "wait", tempo: 80 };

function backend(): FakeBackend {
  return new FakeBackend()
    .answer("session.start", () => ({ seed: 42, tempo: 80, notices: [] }))
    .answer("settings.get", () => ({ input: "virtual:keyboard", keyboards: {}, tempos: {}, latencyOffsetMs: 0 }));
}

const SETUP = { ...defaultSetup({ keyboards: {}, tempos: {}, latencyOffsetMs: 0 }), tempo: 80 };

function show(fake: FakeBackend, onSettings = vi.fn(), setup: Setup = SETUP, onSetupChange = vi.fn()) {
  const view = render(
    <BackendContext.Provider value={fake.client}>
      <PracticeScreen params={PARAMS} setup={setup} onSetupChange={onSetupChange} onChangeSettings={onSettings} />
    </BackendContext.Provider>,
  );
  return { ...view, onSettings, onSetupChange };
}

describe("PracticeScreen", () => {
  it("starts the session and shows what to play", async () => {
    const fake = backend();
    show(fake);
    await waitFor(() => expect(fake.callsOf("session.start")).toEqual([PARAMS]));

    act(() => fake.emit("session.target", { start: "0", notes: [{ pitch: "E4", midi: 64, hand: "RIGHT" }] }));

    expect(screen.getByLabelText("Now playing").textContent).toContain("E4");
  });

  it("names chords", async () => {
    const fake = backend();
    show(fake);
    await waitFor(() => expect(fake.callsOf("session.start")).toHaveLength(1));

    act(() =>
      fake.emit("session.target", {
        start: "0",
        name: "Dm/F",
        notes: [
          { pitch: "F4", midi: 65, hand: "RIGHT" },
          { pitch: "A4", midi: 69, hand: "RIGHT" },
          { pitch: "D5", midi: 74, hand: "RIGHT" },
        ],
      }),
    );

    expect(screen.getByLabelText("Now playing").textContent).toContain("Dm/F");
  });

  it("pauses, resumes and stops", async () => {
    const fake = backend();
    show(fake);
    await waitFor(() => expect(fake.callsOf("session.start")).toHaveLength(1));
    act(() => fake.emit("session.state", { state: "RUNNING" }));

    fireEvent.click(screen.getByRole("button", { name: "Pause" }));
    act(() => fake.emit("session.state", { state: "PAUSED" }));
    fireEvent.click(screen.getByRole("button", { name: "Resume" }));
    fireEvent.click(screen.getByRole("button", { name: "Stop" }));

    expect(fake.calls.map((call) => call.method)).toEqual(
      expect.arrayContaining(["session.pause", "session.resume", "session.stop"]),
    );
  });

  it("changes the tempo while playing", async () => {
    const fake = backend();
    show(fake);
    await waitFor(() => expect(fake.callsOf("session.start")).toHaveLength(1));

    fireEvent.click(screen.getByRole("button", { name: "Faster" }));

    expect(fake.callsOf("session.changeTempo")).toEqual([{ bpm: 81 }]);
  });

  it("plays the virtual keyboard from the computer keyboard", async () => {
    const fake = backend();
    show(fake);
    await waitFor(() => expect(fake.callsOf("session.start")).toHaveLength(1));

    fireEvent.keyDown(window, { key: "d" });
    fireEvent.keyUp(window, { key: "d" });

    expect(fake.callsOf("keyboard.press")).toEqual([{ pitch: "E4" }]);
    expect(fake.callsOf("keyboard.release")).toEqual([{ pitch: "E4" }]);
  });

  it("shows the results and offers to practise again", async () => {
    const fake = backend();
    const { onSettings } = show(fake);
    await waitFor(() => expect(fake.callsOf("session.start")).toHaveLength(1));

    act(() =>
      fake.emit("session.summary", {
        timed: false, completedTargets: 50, correctNotes: 57, wrongKeys: 3, perfect: 0, good: 0, ok: 0, missed: 0,
        accuracyPercent: 95, elapsedMs: 83000,
      }),
    );

    expect(screen.getByLabelText("Accuracy").textContent).toBe("95%");
    expect(screen.getByText("1:23")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Practise again" }));
    await waitFor(() => expect(fake.callsOf("session.start")).toHaveLength(2));
    fireEvent.click(screen.getByRole("button", { name: "Stop" }));
    act(() =>
      fake.emit("session.summary", {
        timed: true, completedTargets: 0, correctNotes: 0, wrongKeys: 1, perfect: 5, good: 2, ok: 1, missed: 2,
        accuracyPercent: 80, elapsedMs: 9000,
      }),
    );
    expect(screen.getByText("Perfect")).toBeTruthy();
    fireEvent.click(screen.getByRole("button", { name: "Change settings" }));
    expect(onSettings).toHaveBeenCalled();
  });

  it("adjusts a random-notes session while playing", async () => {
    const fake = backend();
    const { onSetupChange } = show(fake);
    await waitFor(() => expect(fake.callsOf("session.start")).toHaveLength(1));

    fireEvent.change(screen.getByLabelText("Hands"), { target: { value: "left" } });
    fireEvent.change(screen.getByLabelText("Level"), { target: { value: "2" } });
    fireEvent.click(screen.getByLabelText("Sharps and flats"));
    fireEvent.click(screen.getByLabelText("Mixed note values"));

    expect(fake.callsOf("session.adjust")).toEqual([
      { hands: "left" },
      { level: 2 },
      { accidentals: true },
      { mixed: true },
    ]);
    expect(onSetupChange).toHaveBeenLastCalledWith(expect.objectContaining({ hands: "left", level: 2, mixed: true }));
    expect(screen.queryByLabelText("Dominant 7th")).toBeNull();
  });

  it("adjusts the hands and chord types of a chord session", async () => {
    const fake = backend();
    show(fake, vi.fn(), { ...SETUP, exercise: "chords", chordTypes: ["maj"] });
    await waitFor(() => expect(fake.callsOf("session.start")).toHaveLength(1));
    expect(screen.queryByLabelText("Level")).toBeNull();

    fireEvent.click(screen.getByLabelText("Dominant 7th"));
    fireEvent.change(screen.getByLabelText("Hands"), { target: { value: "both" } });

    expect(fake.callsOf("session.adjust")).toEqual([{ chordTypes: ["maj", "dom7"] }, { hands: "both" }]);
  });

  it("keeps at least one chord type", async () => {
    const fake = backend();
    show(fake, vi.fn(), { ...SETUP, exercise: "chords", chordTypes: ["maj"] });
    await waitFor(() => expect(fake.callsOf("session.start")).toHaveLength(1));

    fireEvent.click(screen.getByLabelText("Major"));

    expect(fake.callsOf("session.adjust")).toEqual([]);
    expect((screen.getByLabelText("Major") as HTMLInputElement).checked).toBe(true);
  });

  it("shows a song's tempo as a share of the song and its notices, without adjusting", async () => {
    const fake = new FakeBackend()
      .answer("session.start", () => ({ seed: 0, tempo: 84, songTempo: 120, notices: ["3 notes outside your keyboard were moved by an octave"] }))
      .answer("settings.get", () => ({ input: "virtual:keyboard", keyboards: {}, tempos: {}, latencyOffsetMs: 0 }));
    render(
      <BackendContext.Provider value={fake.client}>
        <PracticeScreen params={{ input: "virtual:keyboard", exercise: "song", songId: "x" }} onChangeSettings={vi.fn()} />
      </BackendContext.Provider>,
    );

    expect(await screen.findByText("70% of the song")).toBeTruthy();
    expect(screen.getByText("3 notes outside your keyboard were moved by an octave")).toBeTruthy();
    expect(screen.queryByLabelText("Adjust while playing")).toBeNull();
  });

  it("explains a failed start", async () => {
    const fake = backend().fail("session.start", -32002, "MIDI device unavailable: jdk:Piano");
    show(fake);

    expect((await screen.findByRole("alert")).textContent).toContain("MIDI device unavailable");
  });
});
