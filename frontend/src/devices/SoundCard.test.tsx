import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { SoundCard } from "./SoundCard";
import type { SettingsView } from "../api/protocol";

const SETTINGS: SettingsView = { output: "jdk:Gervill", keyboards: {}, tempos: {}, latencyOffsetMs: 0, instrument: 19, keySound: false };

function show(settings: SettingsView = SETTINGS) {
  const onSave = vi.fn();
  const onTry = vi.fn();
  render(<SoundCard settings={settings} onSave={onSave} onTry={onTry} />);
  return { onSave, onTry };
}

describe("SoundCard", () => {
  it("shows the saved instrument among the groups", () => {
    show();

    const select = screen.getByLabelText("Instrument") as HTMLSelectElement;
    expect(select.value).toBe("19");
    expect(screen.getByRole("group", { name: "Space and synth" })).toBeTruthy();
    expect(screen.getByRole("option", { name: "Trumpet" })).toBeTruthy();
  });

  it("saves a chosen instrument", () => {
    const { onSave } = show();

    fireEvent.change(screen.getByLabelText("Instrument"), { target: { value: "56" } });

    expect(onSave).toHaveBeenCalledWith({ instrument: 56 });
  });

  it("switches your keys' sound on and off", () => {
    const { onSave } = show();

    fireEvent.click(screen.getByLabelText("Play my keys through the output"));

    expect(onSave).toHaveBeenCalledWith({ keySound: true });
  });

  it("plays a sample of the instrument", () => {
    const { onTry } = show();

    fireEvent.click(screen.getByRole("button", { name: "Try it" }));

    expect(onTry).toHaveBeenCalledWith(19);
  });

  it("needs an output first", () => {
    show({ ...SETTINGS, output: undefined });

    expect((screen.getByRole("button", { name: "Try it" }) as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByLabelText("Play my keys through the output") as HTMLInputElement).disabled).toBe(true);
    expect(screen.getByText(/Choose an output above/)).toBeTruthy();
  });
});
