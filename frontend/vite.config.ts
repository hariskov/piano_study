import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  base: "./",
  oxc: { include: /\.[cm]?tsx?$/ },
  build: { outDir: "dist/renderer", emptyOutDir: true },
  test: { environment: "jsdom", globals: true, include: ["src/**/*.test.{ts,tsx}", "electron/**/*.test.ts"] },
});
