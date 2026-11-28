import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  base: "./",
  build: { outDir: "dist/renderer", emptyOutDir: true, chunkSizeWarningLimit: 1600 },
  test: { environment: "happy-dom", globals: true, setupFiles: ["src/testing/osmdMock.ts"], include: ["src/**/*.test.{ts,tsx}", "shell/**/*.test.js"] },
});
