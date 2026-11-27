import { useEffect, useState } from "react";
import { useBackend } from "../app/backendContext";
import type { SessionStartParams, SettingsView } from "../api/protocol";
import { TempoControl } from "../tempo/TempoControl";
import { AccidentalsCheck, ChordTypeChecks, HandsSelect, LevelSelect, MixedCheck } from "./exerciseControls";
import { defaultSetup, savedTempo, toStartParams, type Mode, type Setup } from "./setupForm";

interface Props {
  onStart: (params: SessionStartParams, setup: Setup) => void;
  onChooseDevice: () => void;
  previous?: Setup;
}

const KEYS = ["C", "G", "D", "A", "E", "B", "F#", "F", "Bb", "Eb", "Ab", "Db", "Am", "Em", "Bm", "Dm", "Gm", "Cm"];
const INVERSIONS = ["Root position", "1st inversion", "2nd inversion", "3rd inversion (7ths)"];

export function SetupScreen({ onStart, onChooseDevice, previous }: Props) {
  const client = useBackend();
  const [settings, setSettings] = useState<SettingsView>();
  const [setup, setSetup] = useState<Setup>();

  useEffect(() => {
    void client.request("settings.get", {}).then((saved) => {
      setSettings(saved);
      setSetup(previous ?? defaultSetup(saved));
    });
  }, [client, previous]);

  if (!settings || !setup) {
    return <p className="muted">Loading…</p>;
  }
  if (settings.input === undefined) {
    return (
      <section className="screen">
        <h2>Practice</h2>
        <p>Choose your piano (or the virtual keyboard) first.</p>
        <button type="button" className="primary" onClick={onChooseDevice}>
          Choose a device
        </button>
      </section>
    );
  }

  const change = (update: Partial<Setup>) => setSetup({ ...setup, ...update });
  const chooseMode = (mode: Mode) => change({ mode, tempo: savedTempo(settings, mode) });
  const toggle = <T,>(values: T[], value: T) =>
    values.includes(value) ? values.filter((item) => item !== value) : [...values, value];
  const random = setup.exercise === "random";

  return (
    <section className="screen setup">
      <h2>Practice</h2>
      <div className="exercise-tabs" role="tablist" aria-label="Exercise">
        <label className={random ? "tab active" : "tab"}>
          <input type="radio" checked={random} onChange={() => change({ exercise: "random" })} aria-label="Random notes" />
          <span>Random notes</span>
        </label>
        <label className={random ? "tab" : "tab active"}>
          <input type="radio" checked={!random} onChange={() => change({ exercise: "chords" })} aria-label="Chords" />
          <span>Chords</span>
        </label>
      </div>
      <div className="columns">
        <div className="card">
          <div className="segmented" role="radiogroup" aria-label="Follow mode">
            <label>
              <input type="radio" checked={setup.mode === "wait"} onChange={() => chooseMode("wait")} aria-label="Wait mode" />
              <span>Wait for me</span>
            </label>
            <label>
              <input type="radio" checked={setup.mode === "tempo"} onChange={() => chooseMode("tempo")} aria-label="Tempo mode" />
              <span>Keep tempo</span>
            </label>
          </div>
          <TempoControl value={setup.tempo} onChange={(tempo) => change({ tempo })} />
        </div>
        <div className="card form-grid">
          <HandsSelect value={setup.hands} onChange={(hands) => change({ hands })} />
          {random && <LevelSelect value={setup.level} onChange={(level) => change({ level })} />}
          <label className="field">
            <span>Key</span>
            <select aria-label="Key" value={setup.key} onChange={(event) => change({ key: event.target.value })}>
              {KEYS.map((key) => (
                <option key={key} value={key}>
                  {key.endsWith("m") ? `${key.slice(0, -1)} minor` : `${key} major`}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Note value</span>
            <select aria-label="Note value" value={setup.noteValue} onChange={(event) => change({ noteValue: event.target.value as Setup["noteValue"] })}>
              <option value="whole">Whole notes</option>
              <option value="half">Half notes</option>
              <option value="quarter">Quarter notes</option>
              <option value="eighth">Eighth notes</option>
            </select>
          </label>
          <label className="field">
            <span>Time signature</span>
            <select aria-label="Time signature" value={setup.time} onChange={(event) => change({ time: event.target.value })}>
              {["2/4", "3/4", "4/4", "6/8"].map((time) => (
                <option key={time}>{time}</option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Count-in</span>
            <select aria-label="Count-in" value={setup.countIn} onChange={(event) => change({ countIn: Number(event.target.value) })}>
              <option value={0}>Off</option>
              <option value={1}>1 bar</option>
              <option value={2}>2 bars</option>
            </select>
          </label>
          <label className="field">
            <span>Length</span>
            <span className="inline">
              <input
                type="number"
                aria-label="Number of notes"
                min={1}
                value={setup.notes}
                disabled={setup.endless}
                onChange={(event) => change({ notes: Math.max(1, Number(event.target.value)) })}
              />
              <label className="check">
                <input type="checkbox" checked={setup.endless} onChange={(event) => change({ endless: event.target.checked })} />
                Endless
              </label>
            </span>
          </label>
          <label className="field">
            <span>Range (optional)</span>
            <input aria-label="Range" placeholder="e.g. C4-C6" value={setup.range} onChange={(event) => change({ range: event.target.value })} />
          </label>
          <div className="checks">
            <label className="check">
              <input
                type="checkbox"
                checked={setup.metronome && settings.output !== undefined}
                disabled={settings.output === undefined}
                onChange={(event) => change({ metronome: event.target.checked })}
              />
              Metronome {settings.output === undefined && <small className="muted">(choose an output on Devices)</small>}
            </label>
            {random && (
              <>
                <AccidentalsCheck checked={setup.accidentals} onChange={(accidentals) => change({ accidentals })} />
                <MixedCheck checked={setup.mixed} onChange={(mixed) => change({ mixed })} />
              </>
            )}
          </div>
        </div>
        {!random && (
          <div className="card">
            <ChordTypeChecks value={setup.chordTypes} onChange={(chordTypes) => change({ chordTypes })} />
            <fieldset className="plain">
              <legend>Inversions</legend>
              {INVERSIONS.map((name, inversion) => (
                <label key={name} className="check">
                  <input
                    type="checkbox"
                    aria-label={name}
                    checked={setup.inversions.includes(inversion)}
                    onChange={() => change({ inversions: toggle(setup.inversions, inversion) })}
                  />
                  {name}
                </label>
              ))}
            </fieldset>
          </div>
        )}
      </div>
      <div className="actions">
        <button
          type="button"
          className="primary big"
          disabled={!random && (setup.chordTypes.length === 0 || setup.inversions.length === 0)}
          onClick={() => onStart(toStartParams(setup, settings.input!, settings.output), setup)}
        >
          Start
        </button>
      </div>
    </section>
  );
}
