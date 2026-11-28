import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { SheetMusic } from "./SheetMusic";
import { osmdInstances } from "../testing/osmdMock";

describe("SheetMusic", () => {
  it("draws the score and shows the cursor at the position", async () => {
    const { rerender } = render(<SheetMusic xml="<score-partwise/>" position={0.5} />);

    await waitFor(() => expect(osmdInstances).toHaveLength(1));
    const osmd = osmdInstances[0];
    await waitFor(() => expect(osmd.rendered).toBe(true));
    expect(osmd.loaded).toBe("<score-partwise/>");
    expect(osmd.cursor.shown).toBe(true);
    await waitFor(() => expect(osmd.cursor.position).toBe(0.5));

    rerender(<SheetMusic xml="<score-partwise/>" position={1} />);

    await waitFor(() => expect(osmd.cursor.position).toBe(1));
    expect(screen.getByLabelText("Sheet music")).toBeTruthy();
  });

  it("hides the cursor without a position", async () => {
    render(<SheetMusic xml="<score-partwise/>" />);

    await waitFor(() => expect(osmdInstances.at(-1)?.rendered).toBe(true));
    expect(osmdInstances.at(-1)?.cursor.shown).toBe(false);
  });

  it("says when the score can't be drawn", async () => {
    vi.stubGlobal("__osmdFailNextLoad", true);
    render(<SheetMusic xml="broken" />);

    expect(await screen.findByText(/couldn't be drawn/)).toBeTruthy();
    vi.unstubAllGlobals();
  });
});
