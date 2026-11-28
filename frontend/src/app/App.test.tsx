import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { App } from "./App";

describe("App", () => {
  it("explains that it needs the desktop app outside the app window", () => {
    render(<App />);

    expect(screen.getByRole("alert").textContent).toContain("bin/ui");
  });
});
