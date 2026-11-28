import { configure, fireEvent, screen, waitFor } from "@testing-library/dom";
import { runAll, screenshot, test } from "./runner";

declare const SONG_FILE: string;
declare const MIDI_FILE: string;

configure({ asyncUtilTimeout: 10_000 });

const COMPUTER_KEYS: Record<number, string> = {
  60: "a", 61: "w", 62: "s", 63: "e", 64: "d", 65: "f", 66: "t", 67: "g",
  68: "y", 69: "h", 70: "u", 71: "j", 72: "k", 73: "o", 74: "l", 75: "p", 76: ";",
};

// The page updates asynchronously after each event, so every lookup waits for its element.
async function choose(label: string, value: string): Promise<void> {
  fireEvent.change(await screen.findByLabelText(label), { target: { value } });
}

async function click(role: "button", name: string): Promise<void> {
  fireEvent.click(await screen.findByRole(role, { name }));
}

function nowPlaying(): string {
  return screen.getByLabelText("Now playing").textContent!.trim();
}

async function waitForTarget(): Promise<string> {
  await waitFor(() => {
    if (nowPlaying() === "…") throw new Error("no target shown");
  });
  return nowPlaying();
}

function targetKeys(): number[] {
  return [...document.querySelectorAll(".key.target")].map((key) => Number(key.getAttribute("data-midi")));
}

function playTargetsOnComputerKeys(): void {
  const keys = targetKeys();
  keys.forEach((midi) => fireEvent.keyDown(window, { key: COMPUTER_KEYS[midi] }));
  keys.forEach((midi) => fireEvent.keyUp(window, { key: COMPUTER_KEYS[midi] }));
}

function playTargetsByClicking(): void {
  for (const midi of targetKeys()) {
    const key = document.querySelector(`[data-midi="${midi}"]`)!;
    fireEvent.pointerDown(key);
    fireEvent.pointerUp(key);
  }
}

async function expectText(label: string, text: string): Promise<void> {
  await waitFor(() => {
    const shown = screen.getByLabelText(label).textContent;
    if (shown !== text) throw new Error(`${label} shows "${shown}", expected "${text}"`);
  });
}

async function expectSheetMusic(): Promise<void> {
  await waitFor(() => {
    if (!screen.getByLabelText("Sheet music").querySelector("svg")) throw new Error("no sheet music drawn");
  });
}

test("sets up the virtual keyboard on the Devices screen", async () => {
  fireEvent.click(await screen.findByLabelText("Virtual keyboard"));
  await choose("Keyboard size", "61");
  await waitFor(() => {
    const count = document.querySelectorAll("[data-midi]").length;
    if (count !== 61) throw new Error(`${count} keys shown`);
  });
  await screenshot("1-devices");
});

test("plays a short random-notes session to the results", async () => {
  await click("button", "Practice");
  await choose("Count-in", "0");
  await choose("Number of notes", "3");
  await choose("Display", "lanes");
  await screenshot("2-setup");
  await click("button", "Start");

  for (let played = 0; played < 3; played++) {
    await waitForTarget();
    if (played === 1) {
      await screenshot("3-practice");
    }
    playTargetsByClicking();
    if (played < 2) {
      await waitFor(() => {
        if (document.querySelectorAll(".block.correct").length !== played + 1) throw new Error("not marked correct");
      });
    }
  }

  await expectText("Accuracy", "100%");
  await screenshot("4-results");
});

test("plays a chord session with the computer keyboard", async () => {
  await click("button", "Change settings");
  fireEvent.click(await screen.findByText("Chords", { exact: true }));
  await choose("Range", "C4-E5");
  await choose("Number of notes", "1");
  await choose("Display", "staff");
  await click("button", "Start");
  await waitForTarget();
  await expectSheetMusic();
  await screenshot("5-chord");

  playTargetsOnComputerKeys();

  await expectText("Accuracy", "100%");
});

test("imports a song into the library and opens it", async () => {
  window.backend!.chooseSongFile = async () => SONG_FILE;
  await click("button", "Library");
  await screen.findByText(/No songs yet/);

  await click("button", "Import…");

  await screen.findByText('Imported "Two Hands"');
  await screen.findByText("G major");
  await waitFor(() => {
    if (!document.querySelector("code")?.textContent?.includes("musicFiles/xml/Two Hands.musicxml")) throw new Error("no path");
  });
  await screenshot("6-library");
});

test("practises the imported song to the results", async () => {
  await click("button", "Show sheet music");
  await expectSheetMusic();
  await screenshot("6b-library-sheet");
  await click("button", "Practise");
  await choose("Count-in", "0");
  await choose("Play it", "2");
  await screenshot("7-song-setup");
  await click("button", "Start");

  for (let played = 0; played < 4; played++) {
    const shown = await waitForTarget();
    if (played === 0) {
      await expectSheetMusic();
      await screenshot("8-song-practice");
    }
    playTargetsOnComputerKeys();
    if (played < 3) {
      await waitFor(() => {
        if (nowPlaying() === shown) throw new Error("target did not move on");
      });
    }
  }

  await expectText("Accuracy", "100%");
  await waitFor(() => {
    if (!screen.getByRole("list", { name: "Passes" }).textContent?.includes("Pass 2")) throw new Error("no second pass");
  });
  await screenshot("9-song-results");
});

test("imports a MIDI file, leaving out a part", async () => {
  window.backend!.chooseSongFile = async () => MIDI_FILE;
  await click("button", "Library");
  await click("button", "Import…");

  await screen.findByRole("row", { name: "Melody" });
  await screen.findByText("Flute");
  await screenshot("10-midi-import");
  fireEvent.click(screen.getByLabelText("Include Melody"));
  await click("button", "Import 1 part");

  await screen.findByText('Imported "Little Tune"');
  await waitFor(() => {
    if (!document.querySelector(".song-details table.parts")?.textContent?.includes("Piano")) throw new Error("no piano part");
    if (document.querySelector(".song-details table.parts")?.textContent?.includes("Melody")) throw new Error("melody imported");
  });
  await screenshot("11-midi-imported");
});

void runAll();
