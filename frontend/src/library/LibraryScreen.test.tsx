import { describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { LibraryScreen } from "./LibraryScreen";
import { BackendContext } from "../app/backendContext";
import { FakeBackend } from "../testing/fakeBackend";
import type { SongSummary } from "../api/protocol";

const MELODY: SongSummary = {
  id: "simple-melody",
  title: "Simple Melody",
  fileName: "Simple Melody.musicxml",
  partCount: 1,
  measureCount: 8,
  sourceFile: "melody.musicxml",
  format: "MusicXML",
  importedAt: "2026-11-28T08:00:00Z",
  warnings: [],
};
const SONATINA: SongSummary = { ...MELODY, id: "sonatina", title: "Sonatina", fileName: "Sonatina.musicxml", format: "MIDI" };

function backend(songs: SongSummary[] = [MELODY, SONATINA]): FakeBackend {
  return new FakeBackend()
    .answer("library.list", () => ({ songs }))
    .answer("library.open", (params) => ({
      id: params.id!,
      title: "Simple Melody",
      composer: "Anon",
      file: "/home/me/musicFiles/xml/Simple Melody.musicxml",
      measureCount: 8,
      timeSignature: "3/4",
      key: "G major",
      tempo: 96,
      parts: [{ id: "P1", name: "Piano", staffCount: 2, noteCount: 120, lowest: "G2", highest: "D6" }],
      warnings: ["Repeats are played straight through"],
    }));
}

function show(fake: FakeBackend, chooseFile = vi.fn(async () => undefined as string | undefined)) {
  render(
    <BackendContext.Provider value={fake.client}>
      <LibraryScreen chooseFile={chooseFile} />
    </BackendContext.Provider>,
  );
  return chooseFile;
}

describe("LibraryScreen", () => {
  it("lists the songs", async () => {
    show(backend());

    expect(await screen.findByText("Simple Melody")).toBeTruthy();
    expect(screen.getByText("Sonatina")).toBeTruthy();
  });

  it("finds songs by name", async () => {
    show(backend());
    await screen.findByText("Simple Melody");

    fireEvent.change(screen.getByLabelText("Search songs"), { target: { value: "sona" } });

    expect(screen.queryByText("Simple Melody")).toBeNull();
    expect(screen.getByText("Sonatina")).toBeTruthy();
  });

  it("opens a song to show its details", async () => {
    const fake = backend();
    show(fake);

    fireEvent.click(await screen.findByText("Simple Melody"));

    expect(await screen.findByText("/home/me/musicFiles/xml/Simple Melody.musicxml")).toBeTruthy();
    expect(screen.getByText("G major")).toBeTruthy();
    expect(screen.getByText("Repeats are played straight through")).toBeTruthy();
    expect(screen.getByText("G2–D6")).toBeTruthy();
    expect(fake.callsOf("library.open")).toEqual([{ id: "simple-melody" }]);
  });

  it("imports a chosen file and shows it", async () => {
    const fake = backend([]).answer("library.import", () => ({ ...SONATINA, warnings: ["Key estimated as C major"] }));
    const chooseFile = show(fake, vi.fn(async () => "/home/me/Sonatina.mid"));
    await screen.findByText(/No songs yet/);

    fireEvent.click(screen.getByRole("button", { name: "Import…" }));

    await waitFor(() => expect(fake.callsOf("library.import")).toEqual([{ path: "/home/me/Sonatina.mid" }]));
    expect(chooseFile).toHaveBeenCalled();
    expect(await screen.findByText(/Imported "Sonatina"/)).toBeTruthy();
    expect(screen.getByText("Key estimated as C major")).toBeTruthy();
  });

  it("passes the MIDI options when they are changed", async () => {
    const fake = backend([]).answer("library.import", () => SONATINA);
    show(fake, vi.fn(async () => "/home/me/Sonatina.mid"));
    await screen.findByText(/No songs yet/);

    fireEvent.click(screen.getByText("MIDI options"));
    fireEvent.click(screen.getByLabelText("Triplets"));
    fireEvent.change(screen.getByLabelText("Split hands at"), { target: { value: "A3" } });
    fireEvent.click(screen.getByRole("button", { name: "Import…" }));

    await waitFor(() =>
      expect(fake.callsOf("library.import")).toEqual([{ path: "/home/me/Sonatina.mid", grid: 16, triplets: true, splitPoint: "A3" }]),
    );
  });

  it("does nothing when no file is chosen", async () => {
    const fake = backend();
    show(fake);
    await screen.findByText("Simple Melody");

    fireEvent.click(screen.getByRole("button", { name: "Import…" }));

    await act(async () => undefined);
    expect(fake.callsOf("library.import")).toEqual([]);
  });

  it("explains a file it cannot import", async () => {
    const fake = backend().fail("library.import", -32003, "broken.mid: not a readable MIDI file");
    show(fake, vi.fn(async () => "/home/me/broken.mid"));
    await screen.findByText("Simple Melody");

    fireEvent.click(screen.getByRole("button", { name: "Import…" }));

    expect((await screen.findByRole("alert")).textContent).toContain("not a readable MIDI file");
  });

  it("deletes a song after asking", async () => {
    const fake = backend();
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    show(fake);
    fireEvent.click(await screen.findByText("Simple Melody"));
    await screen.findByText("G major");

    fireEvent.click(screen.getByRole("button", { name: "Delete" }));

    await waitFor(() => expect(fake.callsOf("library.delete")).toEqual([{ id: "simple-melody" }]));
    expect(confirm).toHaveBeenCalled();
    confirm.mockRestore();
  });
});
