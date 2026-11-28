import { describe, expect, it } from "vitest";
import type { TargetEvent } from "../api/protocol";
import { STAFF_COLOURS, staffMusicXml, type StaffItem } from "./staffMusicXml";
import { midiOf } from "../music/pitch";

function target(...notes: string[]): TargetEvent {
  return {
    start: "0",
    notes: notes.map((note) => {
      const [pitch, hand = "RIGHT"] = note.split(":");
      return { pitch, midi: midiOf(pitch), hand };
    }),
  };
}

function current(...notes: string[]): StaffItem {
  return { target: target(...notes), state: "current" };
}

function parse(items: StaffItem[]): Document {
  return new DOMParser().parseFromString(staffMusicXml(items), "application/xml");
}

function shown(score: Document, staff: string): string[] {
  return [...score.querySelectorAll("note")]
    .filter((note) => note.querySelector("staff")?.textContent === staff && note.querySelector("pitch"))
    .map((note) => {
      const alter = note.querySelector("alter")?.textContent;
      const accidental = alter === "1" ? "#" : alter === "-1" ? "b" : "";
      return note.querySelector("step")!.textContent + accidental + note.querySelector("octave")!.textContent;
    });
}

describe("staffMusicXml", () => {
  it("writes a grand staff with treble and bass clefs", () => {
    const score = parse([current("E4")]);

    expect(score.querySelector("staves")?.textContent).toBe("2");
    expect([...score.querySelectorAll("clef sign")].map((sign) => sign.textContent)).toEqual(["G", "F"]);
  });

  it("writes sharps and flats with their accidentals", () => {
    const score = parse([{ target: target("C#4"), state: "correct" }, current("Bb4")]);

    expect(shown(score, "1")).toEqual(["C#4", "Bb4"]);
    expect([...score.querySelectorAll("accidental")].map((accidental) => accidental.textContent)).toEqual(["sharp", "flat"]);
  });

  it("stacks the notes of a chord from the lowest up", () => {
    const score = parse([current("G4", "C4", "E4")]);

    expect(shown(score, "1")).toEqual(["C4", "E4", "G4"]);
    expect([...score.querySelectorAll("note")].filter((note) => note.querySelector("chord")).length).toBe(2);
  });

  it("splits one hand's notes at middle C", () => {
    const score = parse([{ target: target("A3"), state: "correct" }, current("C4")]);

    expect(shown(score, "1")).toEqual(["C4"]);
    expect(shown(score, "2")).toEqual(["A3"]);
  });

  it("keeps each hand on its own staff when both hands play", () => {
    const score = parse([current("B3:RIGHT", "D4:LEFT")]);

    expect(shown(score, "1")).toEqual(["B3"]);
    expect(shown(score, "2")).toEqual(["D4"]);
  });

  it("fills the empty staff with hidden rests so both staves line up", () => {
    const score = parse([{ target: target("E4"), state: "correct" }, current("F4")]);

    const rests = [...score.querySelectorAll("note")].filter((note) => note.querySelector("rest"));
    expect(rests.map((rest) => [rest.getAttribute("print-object"), rest.querySelector("staff")?.textContent])).toEqual([
      ["no", "2"],
      ["no", "2"],
    ]);
    expect(score.querySelector("time beats")?.textContent).toBe("2");
  });

  it("colours the notes by how they were played", () => {
    const score = parse([
      { target: target("C4"), state: "correct" },
      { target: target("D4"), state: "missed" },
      current("E4"),
    ]);

    const colours = [...score.querySelectorAll("note")]
      .filter((note) => note.querySelector("pitch"))
      .map((note) => note.querySelector("notehead")?.getAttribute("color"));
    expect(colours).toEqual([STAFF_COLOURS.correct, STAFF_COLOURS.missed, STAFF_COLOURS.current]);
  });
});
