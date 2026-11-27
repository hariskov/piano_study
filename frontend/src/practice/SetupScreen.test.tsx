import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { SetupScreen } from "./SetupScreen";
import { BackendContext } from "../app/backendContext";
import { FakeBackend } from "../testing/fakeBackend";
import { defaultSetup, type Setup } from "./setupForm";

function show(fake: FakeBackend, onStart = vi.fn(), onChooseDevice = vi.fn(), previous?: Setup) {
  render(
    <BackendContext.Provider value={fake.client}>
      <SetupScreen onStart={onStart} onChooseDevice={onChooseDevice} previous={previous} />
    </BackendContext.Provider>,
  );
  return { onStart, onChooseDevice };
}

function backend(input?: string): FakeBackend {
  return new FakeBackend().answer("settings.get", () => ({
    input,
    output: "jdk:Gervill",
    keyboards: {},
    tempos: { wait: 70, tempo: 100 },
    latencyOffsetMs: 0,
  }));
}

describe("SetupScreen", () => {
  it("asks for a device when none is chosen", async () => {
    const { onChooseDevice } = show(backend(undefined));

    fireEvent.click(await screen.findByRole("button", { name: "Choose a device" }));

    expect(onChooseDevice).toHaveBeenCalled();
  });

  it("uses the saved tempo of the chosen mode", async () => {
    show(backend("jdk:Piano"));
    const tempo = (await screen.findByLabelText("Tempo in BPM")) as HTMLInputElement;
    expect(tempo.value).toBe("70");

    fireEvent.click(screen.getByLabelText("Tempo mode"));

    expect((screen.getByLabelText("Tempo in BPM") as HTMLInputElement).value).toBe("100");
  });

  it("shows chord options only for chords", async () => {
    show(backend("jdk:Piano"));
    await screen.findByLabelText("Tempo in BPM");
    expect(screen.queryByLabelText("Dominant 7th")).toBeNull();

    fireEvent.click(screen.getByLabelText("Chords"));

    expect(screen.getByLabelText("Dominant 7th")).toBeTruthy();
    expect(screen.queryByLabelText("Level")).toBeNull();
  });

  it("starts with the chosen settings", async () => {
    const { onStart } = show(backend("jdk:Piano"));
    await screen.findByLabelText("Tempo in BPM");

    fireEvent.change(screen.getByLabelText("Level"), { target: { value: "3" } });
    fireEvent.change(screen.getByLabelText("Hands"), { target: { value: "both" } });
    fireEvent.click(screen.getByRole("button", { name: "Start" }));

    expect(onStart).toHaveBeenCalledWith(
      expect.objectContaining({ input: "jdk:Piano", level: 3, hands: "both", tempo: 70, metronome: "jdk:Gervill" }),
      expect.objectContaining({ level: 3, hands: "both" }),
    );
  });

  it("starts from the previous setup when there is one", async () => {
    const previous = { ...defaultSetup({ keyboards: {}, tempos: {}, latencyOffsetMs: 0 }), countIn: 0, notes: 7 };
    show(backend("jdk:Piano"), vi.fn(), vi.fn(), previous);

    expect(((await screen.findByLabelText("Count-in")) as HTMLSelectElement).value).toBe("0");
    expect((screen.getByLabelText("Number of notes") as HTMLInputElement).value).toBe("7");
  });
});
