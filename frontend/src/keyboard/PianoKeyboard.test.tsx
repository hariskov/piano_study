import { describe, expect, it, vi } from "vitest";
import { fireEvent, render } from "@testing-library/react";
import { PianoKeyboard } from "./PianoKeyboard";

describe("PianoKeyboard", () => {
  it("draws one key for every note of the range", () => {
    const { container } = render(<PianoKeyboard low={60} high={72} />);

    expect(container.querySelectorAll("[data-midi]")).toHaveLength(13);
  });

  it("shows pressed, target and marked keys", () => {
    const { container } = render(
      <PianoKeyboard low={60} high={72} pressed={new Set([60])} targets={new Set([64])} marks={new Map([[62, "wrong"]])} />,
    );

    expect(container.querySelector('[data-midi="60"]')!.getAttribute("class")).toContain("pressed");
    expect(container.querySelector('[data-midi="64"]')!.getAttribute("class")).toContain("target");
    expect(container.querySelector('[data-midi="62"]')!.getAttribute("class")).toContain("wrong");
  });

  it("plays keys pressed with the mouse", () => {
    const onPress = vi.fn();
    const onRelease = vi.fn();
    const { container } = render(<PianoKeyboard low={60} high={72} onPress={onPress} onRelease={onRelease} />);
    const key = container.querySelector('[data-midi="61"]')!;

    fireEvent.pointerDown(key);
    fireEvent.pointerUp(key);

    expect(onPress).toHaveBeenCalledWith(61);
    expect(onRelease).toHaveBeenCalledWith(61);
  });

  it("labels the C keys", () => {
    const { getByText } = render(<PianoKeyboard low={48} high={72} />);

    expect(getByText("C4")).toBeTruthy();
  });
});
