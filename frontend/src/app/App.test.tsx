import { afterEach, describe, expect, it, vi } from "vitest";
import { act, render, screen } from "@testing-library/react";
import { App } from "./App";
import type { BackendBridge } from "../api/backend";

function shellBridge() {
  let exit: (code: number | null) => void = () => {};
  const bridge: BackendBridge = {
    send: () => {},
    onLine: () => {},
    onExit: (listener) => (exit = listener),
    chooseSongFile: async () => undefined,
  };
  vi.stubGlobal("backend", bridge);
  return { exit: (code: number | null) => exit(code) };
}

describe("App", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState(null, "", "/");
  });

  it("explains that it needs the desktop app outside the app window", () => {
    render(<App />);

    expect(screen.getByRole("alert").textContent).toContain("bin/ui");
  });

  it("says when the backend stops and where its log is", () => {
    const shell = shellBridge();
    render(<App />);

    act(() => shell.exit(1));

    const alert = screen.getByRole("alert");
    expect(alert.textContent).toContain("exit code 1");
    expect(alert.textContent).toContain("backend.log");
  });

  it("says when the window restarted the backend after a problem", () => {
    shellBridge();
    window.history.replaceState(null, "", "/?restarted=1#home");

    render(<App />);

    expect(screen.getByRole("status").textContent).toContain("restarted");
  });
  it("shows a picture on every menu button, for children who can't read yet", () => {
    shellBridge();
    render(<App />);

    for (const name of ["Home", "Practice", "Library", "Devices"]) {
      expect(screen.getByRole("button", { name }).querySelector("svg.icon")).not.toBeNull();
    }
  });
});
