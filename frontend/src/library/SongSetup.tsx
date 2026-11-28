import { useEffect, useState } from "react";
import { useBackend } from "../app/backendContext";
import type { SessionStartParams, SettingsView, SongDetails } from "../api/protocol";

interface Props {
  song: SongDetails;
  onStart: (params: SessionStartParams) => void;
  onBack: () => void;
}

const SLOWEST_PERCENT = 25;
const FASTEST_PERCENT = 150;

export function SongSetup({ song, onStart, onBack }: Props) {
  const client = useBackend();
  const [settings, setSettings] = useState<SettingsView>();
  const [part, setPart] = useState(song.parts[0]?.id ?? "");
  const [hands, setHands] = useState("right");
  const [mode, setMode] = useState("wait");
  const [percent, setPercent] = useState(100);
  const [countIn, setCountIn] = useState(1);
  const [metronome, setMetronome] = useState(true);
  const [outOfRange, setOutOfRange] = useState("shift");

  useEffect(() => {
    void client.request("settings.get", {}).then(setSettings);
  }, [client]);

  const chosenPart = song.parts.find((candidate) => candidate.id === part);
  const twoStaves = (chosenPart?.staffCount ?? 1) === 2;
  const choosePart = (id: string) => {
    setPart(id);
    if ((song.parts.find((candidate) => candidate.id === id)?.staffCount ?? 1) < 2) {
      setHands("right");
    }
  };

  if (!settings) {
    return <p className="muted">Loading…</p>;
  }
  const start = () => {
    const params: SessionStartParams = {
      input: settings.input,
      exercise: "song",
      songId: song.id,
      part,
      hands,
      mode,
      tempoPercent: percent,
      countIn,
      outOfRange,
    };
    if (metronome && settings.output) {
      params.metronome = settings.output;
    }
    onStart(params);
  };

  return (
    <section className="screen setup">
      <h2>{song.title}</h2>
      <p className="muted">
        {song.key} · {song.timeSignature} · {song.measureCount} bars
      </p>
      {settings.input === undefined && <p className="problem">Choose your piano on the Devices screen first.</p>}
      <div className="columns">
        <div className="card form-grid">
          <label className="field">
            <span>Part</span>
            <select aria-label="Part" value={part} onChange={(event) => choosePart(event.target.value)}>
              {song.parts.map((candidate) => (
                <option key={candidate.id} value={candidate.id}>
                  {candidate.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Hands</span>
            <select aria-label="Hands" value={hands} onChange={(event) => setHands(event.target.value)}>
              <option value="right">Right hand</option>
              {twoStaves && <option value="left">Left hand</option>}
              {twoStaves && <option value="both">Both hands</option>}
            </select>
          </label>
          <label className="field">
            <span>Follow</span>
            <select aria-label="Follow mode" value={mode} onChange={(event) => setMode(event.target.value)}>
              <option value="wait">Wait for me</option>
              <option value="tempo">Keep tempo</option>
            </select>
          </label>
          <label className="field">
            <span>Count-in</span>
            <select aria-label="Count-in" value={countIn} onChange={(event) => setCountIn(Number(event.target.value))}>
              <option value={0}>Off</option>
              <option value={1}>1 bar</option>
              <option value={2}>2 bars</option>
            </select>
          </label>
          <label className="field">
            <span>Notes outside your keyboard</span>
            <select aria-label="Notes outside your keyboard" value={outOfRange} onChange={(event) => setOutOfRange(event.target.value)}>
              <option value="shift">Move them by an octave</option>
              <option value="skip">Skip them</option>
            </select>
          </label>
          <label className="check">
            <input
              type="checkbox"
              checked={metronome && settings.output !== undefined}
              disabled={settings.output === undefined}
              onChange={(event) => setMetronome(event.target.checked)}
            />
            Metronome
          </label>
        </div>
        <div className="card">
          <label className="field">
            <span>Tempo</span>
            <span className="inline">
              <input
                type="range"
                aria-label="Tempo percent"
                min={SLOWEST_PERCENT}
                max={FASTEST_PERCENT}
                step={5}
                value={percent}
                onChange={(event) => setPercent(Number(event.target.value))}
              />
              <strong>{percent}%</strong>
              <span className="muted">{Math.round((song.tempo * percent) / 100)} BPM</span>
            </span>
          </label>
          <p className="muted">The song is written at {song.tempo} BPM. Start slower and speed up as it gets easier.</p>
        </div>
      </div>
      <div className="actions">
        <button type="button" onClick={onBack}>
          Back
        </button>
        <button type="button" className="primary big" disabled={settings.input === undefined} onClick={start}>
          Start
        </button>
      </div>
    </section>
  );
}
