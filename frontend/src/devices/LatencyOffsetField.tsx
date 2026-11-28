import { useState } from "react";

interface Props {
  value: number;
  onSave: (offsetMs: number) => void;
}

const LIMIT_MS = 500;

export function LatencyOffsetField({ value, onSave }: Props) {
  const [text, setText] = useState(String(value));
  const [problem, setProblem] = useState<string>();

  const apply = () => {
    const offset = Number(text);
    if (text.trim() === "" || !Number.isInteger(offset) || Math.abs(offset) > LIMIT_MS) {
      setProblem(`Use a whole number from -${LIMIT_MS} to ${LIMIT_MS}`);
      return;
    }
    setProblem(undefined);
    if (offset !== value) {
      onSave(offset);
    }
  };

  return (
    <div className="field">
      <label className="field inline">
        <span>Latency offset</span>
        <input
          type="number"
          aria-label="Latency offset in ms"
          min={-LIMIT_MS}
          max={LIMIT_MS}
          step={5}
          value={text}
          onChange={(event) => setText(event.target.value)}
          onBlur={apply}
          onKeyDown={(event) => event.key === "Enter" && apply()}
        />
        <span className="muted">ms</span>
      </label>
      <small className="muted">
        For Tempo mode. If notes you play on the beat are graded late, raise it by that many milliseconds.
      </small>
      {problem && <span className="problem">{problem}</span>}
    </div>
  );
}
