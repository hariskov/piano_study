import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { ICON_NAMES, Icon } from "./Icon";

describe("Icon", () => {
  it("draws every icon as a picture that screen readers skip", () => {
    const { container } = render(
      <>
        {ICON_NAMES.map((name) => (
          <Icon key={name} name={name} />
        ))}
      </>,
    );

    const icons = container.querySelectorAll("svg.icon");
    expect(icons).toHaveLength(ICON_NAMES.length);
    icons.forEach((icon) => {
      expect(icon.getAttribute("aria-hidden")).toBe("true");
      expect(icon.childElementCount).toBeGreaterThan(0);
    });
  });

  it("keeps the button's name to its words", () => {
    render(
      <button type="button">
        <Icon name="play" />
        Start
      </button>,
    );

    expect(screen.getByRole("button", { name: "Start" })).toBeTruthy();
  });
});
