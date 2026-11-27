import { useEffect, useState } from "react";
import { FASTEST_BPM, SLOWEST_BPM, TEMPO_MARKINGS, clampBpm, markingFor } from "./tempoMarking";

interface Props {
  value: number;
  onChange: (bpm: number) => void;
}

const BIG_STEP = 5;

export function TempoControl({ value, onChange }: Props) {
  const [draft, setDraft] = useState(String(value));
  useEffect(() => setDraft(String(value)), [value]);
  const marking = markingFor(value);

  const commitDraft = () => {
    const bpm = clampBpm(Number(draft));
    setDraft(String(bpm));
    onChange(bpm);
  };
  const step = (direction: number) => (event: React.MouseEvent) =>
    onChange(clampBpm(value + direction * (event.shiftKey ? BIG_STEP : 1)));

  return (
    <div className="tempo-control">
      <div className="tempo-presets">
        {TEMPO_MARKINGS.map((preset) => (
          <button
            key={preset.name}
            type="button"
            aria-label={`${preset.name} ${preset.preset}`}
            className={preset === marking ? "preset active" : "preset"}
            onClick={() => onChange(preset.preset)}
          >
            <span>{preset.name}</span>
            <small>{preset.preset}</small>
          </button>
        ))}
      </div>
      <div className="tempo-row">
        <button type="button" aria-label="Slower" title="Slower (Shift: 5 BPM)" onClick={step(-1)}>
          −
        </button>
        <input
          type="range"
          aria-label="Tempo slider"
          min={SLOWEST_BPM}
          max={FASTEST_BPM}
          value={value}
          onChange={(event) => onChange(clampBpm(Number(event.target.value)))}
        />
        <button type="button" aria-label="Faster" title="Faster (Shift: 5 BPM)" onClick={step(1)}>
          +
        </button>
        <input
          className="tempo-number"
          type="number"
          aria-label="Tempo in BPM"
          min={SLOWEST_BPM}
          max={FASTEST_BPM}
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
          onBlur={commitDraft}
          onKeyDown={(event) => event.key === "Enter" && commitDraft()}
        />
        <span className="tempo-unit">BPM</span>
        <output className="tempo-marking" aria-label="Tempo marking">
          {marking.name}
        </output>
      </div>
    </div>
  );
}
