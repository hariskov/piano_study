import type { ChordLabels } from "./chordLabel";
import type { Display, Hands } from "./setupForm";

export const CHORD_TYPES = [
  ["maj", "Major"],
  ["min", "Minor"],
  ["dim", "Diminished"],
  ["aug", "Augmented"],
  ["maj7", "Major 7th"],
  ["min7", "Minor 7th"],
  ["dom7", "Dominant 7th"],
] as const;

const LEVELS = [
  "1 · steps of a 2nd or 3rd",
  "2 · jumps up to an octave",
  "3 · some two-note intervals",
  "4 · intervals and chords",
  "5 · both hands together",
];

export function HandsSelect({ value, onChange }: { value: Hands; onChange: (hands: Hands) => void }) {
  return (
    <label className="field">
      <span>Hands</span>
      <select aria-label="Hands" value={value} onChange={(event) => onChange(event.target.value as Hands)}>
        <option value="right">Right hand</option>
        <option value="left">Left hand</option>
        <option value="both">Both hands</option>
      </select>
    </label>
  );
}

export function LevelSelect({ value, onChange }: { value: number; onChange: (level: number) => void }) {
  return (
    <label className="field">
      <span>Level</span>
      <select aria-label="Level" value={value} onChange={(event) => onChange(Number(event.target.value))}>
        {LEVELS.map((description, index) => (
          <option key={description} value={index + 1}>
            {description}
          </option>
        ))}
      </select>
    </label>
  );
}

interface CheckProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
}

export function AccidentalsCheck({ checked, onChange }: CheckProps) {
  return (
    <label className="check">
      <input type="checkbox" aria-label="Sharps and flats" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      Sharps and flats <small className="muted">(level 4+)</small>
    </label>
  );
}

export function MixedCheck({ checked, onChange }: CheckProps) {
  return (
    <label className="check">
      <input type="checkbox" aria-label="Mixed note values" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      Mixed note values <small className="muted">(level 3+)</small>
    </label>
  );
}

export function ChordTypeChecks({ value, onChange }: { value: readonly string[]; onChange: (types: string[]) => void }) {
  const toggle = (code: string) => {
    if (!value.includes(code)) {
      onChange([...value, code]);
    } else if (value.length > 1) {
      onChange(value.filter((type) => type !== code));
    }
  };
  return (
    <fieldset className="plain">
      <legend>Chord types</legend>
      {CHORD_TYPES.map(([code, name]) => (
        <label key={code} className="check">
          <input type="checkbox" aria-label={name} checked={value.includes(code)} onChange={() => toggle(code)} />
          {name}
        </label>
      ))}
    </fieldset>
  );
}

export function ChordLabelSelect({ value, onChange }: { value: ChordLabels; onChange: (labels: ChordLabels) => void }) {
  return (
    <label className="field">
      <span>Show</span>
      <select aria-label="Show" value={value} onChange={(event) => onChange(event.target.value as ChordLabels)}>
        <option value="both">Chord name and notes</option>
        <option value="name">Chord name only</option>
        <option value="notes">Notes only</option>
      </select>
    </label>
  );
}

export function DisplaySelect({ value, onChange }: { value: Display; onChange: (display: Display) => void }) {
  return (
    <label className="field">
      <span>Display</span>
      <select aria-label="Display" value={value} onChange={(event) => onChange(event.target.value as Display)}>
        <option value="staff">Notes on the staff</option>
        <option value="lanes">Note lanes</option>
      </select>
    </label>
  );
}
