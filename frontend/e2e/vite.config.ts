import { defineConfig } from "vite";
import path from "node:path";

export default defineConfig({
  define: {
    "process.env.NODE_ENV": JSON.stringify("production"),
    SONG_FILE: JSON.stringify(path.resolve(import.meta.dirname, "../../backend/pp-musicxml/src/test/resources/scores/piano.musicxml")),
    MIDI_FILE: JSON.stringify(path.resolve(import.meta.dirname, "fixtures/little-tune.mid")),
  },
  build: {
    outDir: path.resolve(import.meta.dirname, "../dist/e2e"),
    emptyOutDir: true,
    lib: { entry: path.resolve(import.meta.dirname, "smoke.ts"), formats: ["iife"], name: "smoke", fileName: () => "smoke.js" },
  },
});
