import { describe, expect, it } from "vitest";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { DevicesScreen } from "./DevicesScreen";
import { BackendContext } from "../app/backendContext";
import { FakeBackend } from "../testing/fakeBackend";

function backend(): FakeBackend {
  return new FakeBackend()
    .answer("devices.list", () => ({
      inputs: [
        { id: "jdk:Piano", name: "Piano", description: "Digital Piano" },
        { id: "virtual:keyboard", name: "Virtual keyboard", description: "On-screen and computer keyboard input" },
      ],
      outputs: [{ id: "jdk:Gervill", name: "Gervill", description: "Software MIDI Synthesizer" }],
    }))
    .answer("settings.get", () => ({ input: "jdk:Piano", keyboards: { "jdk:Piano": "61" }, tempos: {}, latencyOffsetMs: 0 }))
    .answer("settings.update", (update) => ({ keyboards: {}, tempos: {}, latencyOffsetMs: 0, ...update }));
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
    const fake = backend().answer("settings.get", () => ({ keyboards: {}, tempos: {}, latencyOffsetMs: 0 }));
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
});
