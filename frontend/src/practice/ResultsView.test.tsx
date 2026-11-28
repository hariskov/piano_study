import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { ResultsView } from "./ResultsView";
import type { SummaryEvent } from "../api/protocol";

const SUMMARY: SummaryEvent = {
  timed: false,
  completedTargets: 30,
  correctNotes: 30,
  wrongKeys: 4,
  perfect: 0,
  good: 0,
  ok: 0,
  missed: 0,
  accuracyPercent: 88,
  elapsedMs: 60000,
  passes: [],
};

describe("ResultsView", () => {
  it("shows the accuracy of each pass of a repeated section", () => {
    render(<ResultsView summary={{ ...SUMMARY, passes: [70, 90, 100] }} onAgain={() => {}} onChangeSettings={() => {}} />);

    const passes = screen.getByRole("list", { name: "Passes" });
    expect(passes.textContent).toContain("Pass 1");
    expect(passes.textContent).toContain("70%");
    expect(passes.textContent).toContain("100%");
  });

  it("leaves the passes out when nothing was repeated", () => {
    render(<ResultsView summary={SUMMARY} onAgain={() => {}} onChangeSettings={() => {}} />);

    expect(screen.queryByRole("list", { name: "Passes" })).toBeNull();
  });

  it("compares with the previous session and names the bars with the most mistakes", () => {
    const past = (accuracyPercent: number, mistakesByBar: Record<string, number>) => ({
      finishedAt: "2026-11-28T10:00:00Z", exercise: "song", songId: "s", hands: "right", mode: "wait", tempo: 90,
      accuracyPercent, timed: false, completedTargets: 30, wrongKeys: 2, perfect: 0, good: 0, ok: 0, missed: 0,
      elapsedMs: 1000, passes: [], mistakesByBar,
    });
    render(
      <ResultsView
        summary={SUMMARY}
        history={[past(70, {}), past(88, { "6": 3, "7": 1 })]}
        onAgain={() => {}}
        onChangeSettings={() => {}}
      />,
    );

    expect(screen.getByText("Last time 70% · now 88%")).toBeTruthy();
    expect(screen.getByText("Most mistakes in bars 6, 7")).toBeTruthy();
  });

  it("says when it is the first time", () => {
    render(<ResultsView summary={SUMMARY} history={[]} onAgain={() => {}} onChangeSettings={() => {}} />);

    expect(screen.queryByText(/Last time/)).toBeNull();
  });
});

