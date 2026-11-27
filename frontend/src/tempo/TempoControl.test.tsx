import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { TempoControl } from "./TempoControl";

describe("TempoControl", () => {
  it("shows the value and its marking", () => {
    render(<TempoControl value={95} onChange={() => {}} />);

    expect((screen.getByLabelText("Tempo in BPM") as HTMLInputElement).value).toBe("95");
    expect((screen.getByLabelText("Tempo slider") as HTMLInputElement).value).toBe("95");
    expect(screen.getByLabelText("Tempo marking").textContent).toBe("Andante");
  });

  it("jumps to a preset", () => {
    const onChange = vi.fn();
    render(<TempoControl value={95} onChange={onChange} />);

    fireEvent.click(screen.getByRole("button", { name: "Allegro 130" }));

    expect(onChange).toHaveBeenCalledWith(130);
  });

  it("steps by one, or by five with Shift", () => {
    const onChange = vi.fn();
    render(<TempoControl value={95} onChange={onChange} />);

    fireEvent.click(screen.getByRole("button", { name: "Slower" }));
    fireEvent.click(screen.getByRole("button", { name: "Faster" }), { shiftKey: true });

    expect(onChange.mock.calls).toEqual([[94], [100]]);
  });

  it("keeps typed and slid values between 20 and 240", () => {
    const onChange = vi.fn();
    render(<TempoControl value={95} onChange={onChange} />);

    fireEvent.change(screen.getByLabelText("Tempo slider"), { target: { value: "150" } });
    fireEvent.change(screen.getByLabelText("Tempo in BPM"), { target: { value: "999" } });
    fireEvent.blur(screen.getByLabelText("Tempo in BPM"));

    expect(onChange.mock.calls).toEqual([[150], [240]]);
  });

  it("never steps past the limits", () => {
    const onChange = vi.fn();
    render(<TempoControl value={240} onChange={onChange} />);

    fireEvent.click(screen.getByRole("button", { name: "Faster" }));

    expect(onChange).toHaveBeenCalledWith(240);
  });
});
