import type { NoteInfo, TargetEvent } from "../api/protocol";

export interface StaffItem {
  readonly target: TargetEvent;
  readonly state: "correct" | "missed" | "current";
}

export const STAFF_COLOURS: Record<StaffItem["state"], string> = {
  correct: "#2E9E5B",
  missed: "#D64545",
  current: "#1F5FBF",
};

const MIDDLE_C = 60;
const PITCH = /^([A-G])([#b]?)(-?\d+)$/;
const ACCIDENTALS: Record<string, { alter: number; name: string }> = {
  "#": { alter: 1, name: "sharp" },
  b: { alter: -1, name: "flat" },
};

type Staff = 1 | 2;

function noteXml(note: NoteInfo, staff: Staff, colour: string, inChord: boolean): string {
  const [, step, accidental, octave] = PITCH.exec(note.pitch)!;
  const sign = ACCIDENTALS[accidental];
  return (
    `<note>${inChord ? "<chord/>" : ""}<pitch><step>${step}</step>${sign ? `<alter>${sign.alter}</alter>` : ""}` +
    `<octave>${octave}</octave></pitch><duration>1</duration><voice>${staff === 1 ? 1 : 5}</voice><type>quarter</type>` +
    `${sign ? `<accidental>${sign.name}</accidental>` : ""}<notehead color="${colour}">normal</notehead><staff>${staff}</staff></note>`
  );
}

function hiddenRest(staff: Staff): string {
  return `<note print-object="no"><rest/><duration>1</duration><voice>${staff === 1 ? 1 : 5}</voice><type>quarter</type><staff>${staff}</staff></note>`;
}

function staffXml(items: readonly StaffItem[], staff: Staff, staffOf: (note: NoteInfo) => Staff): string {
  return items
    .map(({ target, state }) => {
      const notes = target.notes.filter((note) => staffOf(note) === staff).sort((a, b) => a.midi - b.midi);
      if (notes.length === 0) {
        return hiddenRest(staff);
      }
      return notes.map((note, index) => noteXml(note, staff, STAFF_COLOURS[state], index > 0)).join("");
    })
    .join("");
}

// One hand's notes are split at middle C, as a pianist reads them; with both hands each keeps its staff.
function staffChooser(items: readonly StaffItem[]): (note: NoteInfo) => Staff {
  const hands = new Set(items.flatMap(({ target }) => target.notes.map((note) => note.hand)));
  if (hands.size > 1) {
    return (note) => (note.hand === "LEFT" ? 2 : 1);
  }
  return (note) => (note.midi >= MIDDLE_C ? 1 : 2);
}

// A single unmetred bar: each target is a quarter note, the time signature is hidden.
export function staffMusicXml(items: readonly StaffItem[]): string {
  const staffOf = staffChooser(items);
  const beats = Math.max(items.length, 1);
  return (
    `<?xml version="1.0" encoding="UTF-8"?>` +
    `<score-partwise version="4.0"><part-list><score-part id="P1"><part-name print-object="no">Piano</part-name></score-part></part-list>` +
    `<part id="P1"><measure number="1"><attributes><divisions>1</divisions><key><fifths>0</fifths></key>` +
    `<time print-object="no"><beats>${beats}</beats><beat-type>4</beat-type></time><staves>2</staves>` +
    `<clef number="1"><sign>G</sign><line>2</line></clef><clef number="2"><sign>F</sign><line>4</line></clef></attributes>` +
    staffXml(items, 1, staffOf) +
    `<backup><duration>${items.length}</duration></backup>` +
    staffXml(items, 2, staffOf) +
    `</measure></part></score-partwise>`
  );
}
