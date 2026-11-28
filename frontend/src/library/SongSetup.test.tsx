import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { SongSetup } from "./SongSetup";
import { BackendContext } from "../app/backendContext";
import { FakeBackend } from "../testing/fakeBackend";
import type { SongDetails } from "../api/protocol";

const SONG: SongDetails = {
  id: "sonatina",
  title: "Sonatina",
  file: "/x/Sonatina.musicxml",
  measureCount: 24,
  timeSignature: "4/4",
  key: "C major",
  tempo: 120,
  parts: [
    { id: "P1", name: "Piano", staffCount: 2, noteCount: 300, lowest: "C2", highest: "G6" },
    { id: "P2", name: "Flute", staffCount: 1, noteCount: 80, lowest: "D5", highest: "A6" },
  ],
  warnings: [],
};

function show(onStart = vi.fn()) {
  const fake = new FakeBackend().answer("settings.get", () => ({
    input: "jdk:Piano",
    output: "jdk:Gervill",
    keyboards: {},
    tempos: {},
    latencyOffsetMs: 0,
    instrument: 0,
    keySound: false,
  }));
  render(
    <BackendContext.Provider value={fake.client}>
      <SongSetup song={SONG} onStart={onStart} onBack={() => {}} />
    </BackendContext.Provider>,
  );
  return onStart;
}

describe("SongSetup", () => {
  it("starts the song with the chosen part, hands and tempo", async () => {
    const onStart = show();
    await screen.findByText("Sonatina");

    fireEvent.change(screen.getByLabelText("Hands"), { target: { value: "both" } });
    fireEvent.change(screen.getByLabelText("Tempo percent"), { target: { value: "70" } });
    fireEvent.click(screen.getByRole("button", { name: "Start" }));

    expect(onStart).toHaveBeenCalledWith(
      expect.objectContaining({
        input: "jdk:Piano",
        exercise: "song",
        songId: "sonatina",
        part: "P1",
        hands: "both",
        tempoPercent: 70,
        metronome: "jdk:Gervill",
        outOfRange: "shift",
      }),
    );
  });

  it("shows the tempo in BPM next to the percentage", async () => {
    show();
    await screen.findByText("Sonatina");

    fireEvent.change(screen.getByLabelText("Tempo percent"), { target: { value: "50" } });

    expect(screen.getByText("60 BPM")).toBeTruthy();
  });

  it("offers only the right hand for a one-staff part", async () => {
    const onStart = show();
    await screen.findByText("Sonatina");

    fireEvent.change(screen.getByLabelText("Part"), { target: { value: "P2" } });

    expect((screen.getByLabelText("Hands") as HTMLSelectElement).options).toHaveLength(1);
    fireEvent.click(screen.getByRole("button", { name: "Start" }));
    expect(onStart).toHaveBeenCalledWith(expect.objectContaining({ part: "P2", hands: "right" }));
  });

  it("can skip notes outside the keyboard", async () => {
    const onStart = show();
    await screen.findByText("Sonatina");

    fireEvent.change(screen.getByLabelText("Notes outside your keyboard"), { target: { value: "skip" } });
    fireEvent.click(screen.getByRole("button", { name: "Start" }));

    expect(onStart).toHaveBeenCalledWith(expect.objectContaining({ outOfRange: "skip" }));
  });

  it("plays the other parts through the output unless turned off", async () => {
    const onStart = show();
    await screen.findByText("Sonatina");

    fireEvent.click(screen.getByRole("button", { name: "Start" }));
    expect(onStart).toHaveBeenLastCalledWith(expect.objectContaining({ playback: "jdk:Gervill" }));

    fireEvent.click(screen.getByLabelText("Play the other parts"));
    fireEvent.click(screen.getByRole("button", { name: "Start" }));
    expect(onStart.mock.lastCall?.[0].playback).toBeUndefined();
  });

  it("leaves the other hand silent when asked", async () => {
    const onStart = show();
    await screen.findByText("Sonatina");
    expect((screen.getByLabelText("Play the other hand") as HTMLInputElement).checked).toBe(true);

    fireEvent.click(screen.getByLabelText("Play the other hand"));
    fireEvent.click(screen.getByRole("button", { name: "Start" }));

    expect(onStart).toHaveBeenLastCalledWith(expect.objectContaining({ playOtherHand: false }));
  });

  it("has no other hand to play when both hands are yours", async () => {
    show();
    await screen.findByText("Sonatina");

    fireEvent.change(screen.getByLabelText("Hands"), { target: { value: "both" } });

    expect(screen.queryByLabelText("Play the other hand")).toBeNull();
  });

  it("sets how loud the accompaniment plays", async () => {
    const onStart = show();
    await screen.findByText("Sonatina");

    fireEvent.change(screen.getByLabelText("Accompaniment volume"), { target: { value: "90" } });
    fireEvent.click(screen.getByRole("button", { name: "Start" }));

    expect(onStart).toHaveBeenLastCalledWith(expect.objectContaining({ playback: "jdk:Gervill", playbackVolume: 90 }));
  });

  it("offers listening and playing along", async () => {
    const onStart = show();
    await screen.findByText("Sonatina");

    fireEvent.change(screen.getByLabelText("Follow mode"), { target: { value: "listen" } });
    fireEvent.click(screen.getByRole("button", { name: "Start" }));

    expect(onStart).toHaveBeenCalledWith(expect.objectContaining({ mode: "listen", playback: "jdk:Gervill" }));
    expect(screen.getByRole("option", { name: "Play along (no scoring)" })).toBeTruthy();
  });

  it("practises a section of bars several times", async () => {
    const onStart = show();
    await screen.findByText("Sonatina");

    fireEvent.change(screen.getByLabelText("From bar"), { target: { value: "5" } });
    fireEvent.change(screen.getByLabelText("To bar"), { target: { value: "8" } });
    fireEvent.change(screen.getByLabelText("Play it"), { target: { value: "5" } });
    fireEvent.click(screen.getByRole("button", { name: "Start" }));

    expect(onStart).toHaveBeenCalledWith(expect.objectContaining({ fromBar: 5, toBar: 8, repeats: 5 }));
  });

  it("plays the whole song once by default and keeps the bars in order", async () => {
    const onStart = show();
    await screen.findByText("Sonatina");

    fireEvent.change(screen.getByLabelText("From bar"), { target: { value: "30" } });
    fireEvent.click(screen.getByRole("button", { name: "Start" }));

    expect(onStart).toHaveBeenCalledWith(expect.objectContaining({ fromBar: 24, toBar: 24, repeats: 1 }));
  });
});

