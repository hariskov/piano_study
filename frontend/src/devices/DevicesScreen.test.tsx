import { describe, expect, it } from "vitest";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { DevicesScreen } from "./DevicesScreen";
import { BackendContext } from "../app/backendContext";
import { FakeBackend } from "../testing/fakeBackend";
import type { SettingsView } from "../api/protocol";

function backend(): FakeBackend {
  let saved: SettingsView = { input: "jdk:Piano", output: "jdk:Gervill", keyboards: { "jdk:Piano": "61" }, tempos: {}, latencyOffsetMs: 0, instrument: 0, keySound: false };
  return new FakeBackend()
    .answer("devices.list", () => ({
      inputs: [
        { id: "jdk:Piano", name: "Piano", description: "Digital Piano" },
        { id: "virtual:keyboard", name: "Virtual keyboard", description: "On-screen and computer keyboard input" },
      ],
      outputs: [{ id: "jdk:Gervill", name: "Gervill", description: "Software MIDI Synthesizer" }],
    }))
    .answer("settings.get", () => saved)
    .answer("settings.update", (update) => {
      saved = { ...saved, ...update, keyboards: { ...saved.keyboards, ...update.keyboards } } as SettingsView;
      return saved;
    });
}

function show(fake: FakeBackend) {
  return render(
    <BackendContext.Provider value={fake.client}>
      <DevicesScreen />
    </BackendContext.Provider>,
  );
}

describe("DevicesScreen", () => {
  it("lists the inputs and marks the saved one", async () => {
    show(backend());

    const piano = await screen.findByLabelText("Piano");
    expect((piano as HTMLInputElement).checked).toBe(true);
    expect(screen.getByLabelText("Virtual keyboard")).toBeTruthy();
  });

  it("monitors the chosen input and lights up pressed keys", async () => {
    const fake = backend();
    const { container } = show(fake);
    await screen.findByLabelText("Piano");

    await waitFor(() => expect(fake.callsOf("devices.monitor")).toEqual([{ input: "jdk:Piano" }]));
    act(() => fake.emit("input", { type: "NOTE_ON", pitch: "C4", midi: 60, velocity: 70, t: 1 }));

    expect(container.querySelector('[data-midi="60"]')!.getAttribute("class")).toContain("pressed");
  });

  it("saves the chosen input and its keyboard size", async () => {
    const fake = backend();
    show(fake);
    await screen.findByLabelText("Piano");

    fireEvent.click(screen.getByLabelText("Virtual keyboard"));
    fireEvent.change(screen.getByLabelText("Keyboard size"), { target: { value: "49" } });

    await waitFor(() =>
      expect(fake.callsOf("settings.update")).toContainEqual({ keyboards: { "virtual:keyboard": "49" } }),
    );
    expect(fake.callsOf("settings.update")).toContainEqual({ input: "virtual:keyboard" });
  });

  it("saves the input it shows as chosen when none was saved", async () => {
    const fake = backend().answer("settings.get", () => ({ keyboards: {}, tempos: {}, latencyOffsetMs: 0, instrument: 0, keySound: false }));
    show(fake);
    await screen.findByLabelText("Piano");

    await waitFor(() => expect(fake.callsOf("settings.update")).toEqual([{ input: "jdk:Piano" }]));
  });

  it("rescans for devices when asked", async () => {
    const fake = backend().answer("devices.rescan", () => ({
      inputs: [{ id: "jdk:Piano2", name: "Piano 2", description: "Plugged in later" }],
      outputs: [],
    }));
    show(fake);
    await screen.findByLabelText("Piano");

    fireEvent.click(screen.getByRole("button", { name: "Rescan" }));

    expect(await screen.findByLabelText("Piano 2")).toBeTruthy();
    expect(fake.callsOf("devices.rescan")).toHaveLength(1);
  });

  it("tells you to plug in the piano and rescan when there is no input", async () => {
    show(backend().answer("devices.list", () => ({ inputs: [], outputs: [] })));

    expect(await screen.findByText(/Plug in the piano, then press Rescan/)).toBeTruthy();
  });

  it("shows the saved keyboard size of the input", async () => {
    show(backend());
    await screen.findByLabelText("Piano");

    expect((screen.getByLabelText("Keyboard size") as HTMLSelectElement).value).toBe("61");
  });

  it("recognises a preset size saved as its range, as the backend gives it back", async () => {
    const fake = backend();
    await fake.client.request("settings.update", { keyboards: { "jdk:Piano": "C2-C7" } });
    show(fake);
    await screen.findByLabelText("Piano");

    expect((screen.getByLabelText("Keyboard size") as HTMLSelectElement).value).toBe("61");
    expect(screen.queryByLabelText("Custom range")).toBeNull();
  });

  it("detects the keyboard size from the lowest and highest key pressed", async () => {
    const fake = backend();
    show(fake);
    await screen.findByText("Digital Piano");

    fireEvent.click(screen.getByRole("button", { name: "Detect from the piano" }));
    expect(screen.getByText("Press the lowest key on your piano.")).toBeTruthy();
    act(() => fake.emit("input", { type: "NOTE_ON", pitch: "E1", midi: 28, velocity: 70, t: 1 }));
    expect(screen.getByText("Now press the highest key.")).toBeTruthy();
    act(() => fake.emit("input", { type: "NOTE_ON", pitch: "G7", midi: 103, velocity: 70, t: 2 }));

    await waitFor(() => expect(fake.callsOf("settings.update")).toContainEqual({ keyboards: { "jdk:Piano": "E1-G7" } }));
  });
  it("saves the latency offset for Tempo mode", async () => {
    const fake = backend();
    show(fake);
    const offset = (await screen.findByLabelText("Latency offset in ms")) as HTMLInputElement;
    expect(offset.value).toBe("0");

    fireEvent.change(offset, { target: { value: "35" } });
    fireEvent.blur(offset);

    await waitFor(() => expect(fake.callsOf("settings.update")).toContainEqual({ latencyOffsetMs: 35 }));
  });

  it("refuses a latency offset outside -500 to 500 ms", async () => {
    const fake = backend();
    show(fake);
    const offset = await screen.findByLabelText("Latency offset in ms");

    fireEvent.change(offset, { target: { value: "900" } });
    fireEvent.blur(offset);

    expect(screen.getByText("Use a whole number from -500 to 500")).toBeTruthy();
    expect(fake.callsOf("settings.update").some((update) => "latencyOffsetMs" in (update as object))).toBe(false);
  });
  it("plays a sample of the instrument and listens again with the new sound", async () => {
    const fake = backend().answer("sound.test", () => null);
    show(fake);
    await screen.findByLabelText("Piano");
    await waitFor(() => expect(fake.callsOf("devices.monitor")).toHaveLength(1));

    fireEvent.change(screen.getByLabelText("Instrument"), { target: { value: "89" } });
    await waitFor(() => expect((screen.getByLabelText("Instrument") as HTMLSelectElement).value).toBe("89"));
    fireEvent.click(screen.getByRole("button", { name: "Try it" }));

    await waitFor(() => expect(fake.callsOf("sound.test")).toEqual([{ instrument: 89 }]));
    expect(fake.callsOf("settings.update")).toContainEqual({ instrument: 89 });
    await waitFor(() => expect(fake.callsOf("devices.monitor")).toHaveLength(2));
  });
});
