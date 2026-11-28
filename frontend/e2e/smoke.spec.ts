import { _electron as electron, expect, test, type ElectronApplication, type Page } from "@playwright/test";
import { mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";

let app: ElectronApplication;
let page: Page;
const screenshots = path.join("test-results", "screens");

test.beforeAll(async () => {
  app = await electron.launch({
    args: [".", "--no-sandbox", "--ozone-platform=x11"],
    env: {
      ...process.env,
      XDG_CONFIG_HOME: mkdtempSync(path.join(tmpdir(), "pp-e2e-")),
      XDG_DATA_HOME: mkdtempSync(path.join(tmpdir(), "pp-e2e-data-")),
      PP_SCREEN: "devices",
    },
  });
  page = await app.firstWindow();
  await page.setViewportSize({ width: 1280, height: 860 });
});

test.afterAll(async () => {
  await app.close();
});

async function playShownTargetByClicking(): Promise<void> {
  const shown = (await page.getByLabel("Now playing").textContent())!.trim();
  for (const pitch of shown.split(" ")) {
    const midi = await page.evaluate((name) => {
      const match = /^([A-G])(#?)(-?\d)$/.exec(name)!;
      const naturals: Record<string, number> = { C: 0, D: 2, E: 4, F: 5, G: 7, A: 9, B: 11 };
      return (Number(match[3]) + 1) * 12 + naturals[match[1]] + (match[2] ? 1 : 0);
    }, pitch);
    await page.locator(`[data-midi="${midi}"]`).dispatchEvent("pointerdown");
    await page.locator(`[data-midi="${midi}"]`).dispatchEvent("pointerup");
  }
}

test("sets up the virtual keyboard on the Devices screen", async () => {
  await page.getByLabel("Virtual keyboard").check();
  await page.getByLabel("Keyboard size").selectOption("61");
  await expect(page.locator("[data-midi]")).toHaveCount(61);
  await page.screenshot({ path: path.join(screenshots, "1-devices.png") });
});

test("plays a short random-notes session to the results", async () => {
  await page.getByRole("button", { name: "Practice" }).click();
  await page.getByLabel("Count-in").selectOption("0");
  await page.getByLabel("Number of notes").fill("3");
  await page.screenshot({ path: path.join(screenshots, "2-setup.png") });
  await page.getByRole("button", { name: "Start" }).click();

  for (let played = 0; played < 3; played++) {
    await expect(page.getByLabel("Now playing")).not.toHaveText("…");
    if (played === 1) {
      await page.screenshot({ path: path.join(screenshots, "3-practice.png") });
    }
    const before = await page.getByLabel("Now playing").textContent();
    await playShownTargetByClicking();
    if (played < 2) {
      await expect(page.locator(".block.correct")).toHaveCount(played + 1);
      expect(before).toBeTruthy();
    }
  }

  await expect(page.getByLabel("Accuracy")).toHaveText("100%");
  await page.screenshot({ path: path.join(screenshots, "4-results.png") });
});

test("plays a chord session with the computer keyboard", async () => {
  await page.getByRole("button", { name: "Change settings" }).click();
  await page.getByText("Chords", { exact: true }).click();
  await page.getByLabel("Range").fill("C4-E5");
  await page.getByLabel("Number of notes").fill("1");
  await page.getByRole("button", { name: "Start" }).click();
  await expect(page.getByLabel("Now playing")).not.toHaveText("…");
  await page.screenshot({ path: path.join(screenshots, "5-chord.png") });

  const keys: Record<number, string> = {
    60: "a", 61: "w", 62: "s", 63: "e", 64: "d", 65: "f", 66: "t", 67: "g",
    68: "y", 69: "h", 70: "u", 71: "j", 72: "k", 73: "o", 74: "l", 75: "p", 76: ";",
  };
  const targets = await page.locator(".key.target").evaluateAll((elements) =>
    elements.map((element) => Number(element.getAttribute("data-midi"))),
  );
  for (const midi of targets) {
    await page.keyboard.down(keys[midi]);
  }
  for (const midi of targets) {
    await page.keyboard.up(keys[midi]);
  }

  await expect(page.getByLabel("Accuracy")).toHaveText("100%");
});

test("imports a song into the library and opens it", async () => {
  const score = path.resolve("../backend/pp-musicxml/src/test/resources/scores/piano.musicxml");
  await app.evaluate(({ dialog }, file) => {
    dialog.showOpenDialog = (async () => ({ canceled: false, filePaths: [file] })) as typeof dialog.showOpenDialog;
  }, score);
  await page.getByRole("button", { name: "Library" }).click();
  await expect(page.getByText(/No songs yet/)).toBeVisible();

  await page.getByRole("button", { name: "Import…" }).click();

  await expect(page.getByText('Imported "Two Hands"')).toBeVisible();
  await expect(page.getByText("G major")).toBeVisible();
  await expect(page.locator("code").first()).toContainText("musicFiles/xml/Two Hands.musicxml");
  await page.screenshot({ path: path.join(screenshots, "6-library.png") });
});

