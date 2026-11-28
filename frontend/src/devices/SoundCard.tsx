import type { SettingsUpdate, SettingsView } from "../api/protocol";
import { INSTRUMENT_GROUPS } from "./instruments";

interface Props {
  settings: SettingsView;
  onSave: (update: SettingsUpdate) => void;
  onTry: (instrument: number) => void;
}

export function SoundCard({ settings, onSave, onTry }: Props) {
  const noOutput = settings.output === undefined;
  return (
    <div className="card sound-card">
      <h3>Sound</h3>
      <label className="field">
        <span>Instrument</span>
        <select aria-label="Instrument" value={settings.instrument} onChange={(event) => onSave({ instrument: Number(event.target.value) })}>
          {INSTRUMENT_GROUPS.map((group) => (
            <optgroup key={group.name} label={group.name}>
              {group.instruments.map(([program, name]) => (
                <option key={program} value={program}>
                  {name}
                </option>
              ))}
            </optgroup>
          ))}
        </select>
      </label>
      <label className="check">
        <input
          type="checkbox"
          aria-label="Play my keys through the output"
          checked={settings.keySound}
          disabled={noOutput}
          onChange={(event) => onSave({ keySound: event.target.checked })}
        />
        Play my keys through the output
      </label>
      <div className="sound-try">
        <button type="button" disabled={noOutput} onClick={() => onTry(settings.instrument)}>
          Try it
        </button>
        <small className="muted">
          {noOutput
            ? "Choose an output above to hear the instruments."
            : "A digital piano still plays its own sound too; turn its volume down to hear only this instrument."}
        </small>
      </div>
    </div>
  );
}
