import { useEffect, useState } from "react";
import { useBackend } from "../app/backendContext";
import type { ImportParams, MidiPreview, SongSummary } from "../api/protocol";

interface Props {
  path: string;
  onImported: (song: SongSummary) => void;
  onCancel: () => void;
}

interface MidiOptions {
  grid: number;
  triplets: boolean;
  splitPoint: string;
}

const DEFAULT_OPTIONS: MidiOptions = { grid: 16, triplets: false, splitPoint: "C4" };

export function isMidiFile(path: string): boolean {
  return /\.midi?$/i.test(path);
}

export function MidiImport({ path, onImported, onCancel }: Props) {
  const client = useBackend();
  const [options, setOptions] = useState(DEFAULT_OPTIONS);
  const [preview, setPreview] = useState<MidiPreview>();
  const [excluded, setExcluded] = useState<ReadonlySet<string>>(new Set());
  const [problem, setProblem] = useState<string>();
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let current = true;
    client.request("library.preview", { path, ...options }).then(
      (result) => {
        if (current) {
          setPreview(result);
          setProblem(undefined);
        }
      },
      (error: Error) => current && setProblem(error.message),
    );
    return () => {
      current = false;
    };
  }, [client, path, options]);

  const change = (update: Partial<MidiOptions>) => setOptions({ ...options, ...update });
  const toggle = (id: string) => {
    const next = new Set(excluded);
    if (!next.delete(id)) {
      next.add(id);
    }
    setExcluded(next);
  };
  const included = preview?.parts.filter((part) => !excluded.has(part.id)).length ?? 0;

  const importFile = async () => {
    setBusy(true);
    setProblem(undefined);
    const params: ImportParams = { path, ...options, ...(excluded.size > 0 && { exclude: [...excluded] }) };
    try {
      onImported(await client.request("library.import", params));
    } catch (error) {
      setProblem((error as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="card midi-import" aria-label="Import a MIDI file">
      <h3>Import {preview?.title ?? path.split("/").pop()}</h3>
      <p className="muted file">
        <code>{path}</code>
      </p>
      <div className="midi-options">
        <label className="field inline">
          <span>Snap notes to</span>
          <select aria-label="Grid" value={options.grid} onChange={(event) => change({ grid: Number(event.target.value) })}>
            {[4, 8, 16, 32].map((grid) => (
              <option key={grid} value={grid}>
                1/{grid}
              </option>
            ))}
          </select>
        </label>
        <label className="check">
          <input type="checkbox" aria-label="Triplets" checked={options.triplets} onChange={(event) => change({ triplets: event.target.checked })} />
          Triplets
        </label>
        <label className="field inline">
          <span>Split hands at</span>
          <input aria-label="Split hands at" value={options.splitPoint} onChange={(event) => change({ splitPoint: event.target.value })} />
        </label>
      </div>
      {problem && (
        <p role="alert" className="problem">
          {problem}
        </p>
      )}
      {preview && (
        <>
          <dl className="facts">
            <div>
              <dt>Bars</dt>
              <dd>{preview.measureCount}</dd>
            </div>
            <div>
              <dt>Time</dt>
              <dd>{preview.timeSignature}</dd>
            </div>
            <div>
              <dt>Key</dt>
              <dd>{preview.key}</dd>
            </div>
            <div>
              <dt>Tempo</dt>
              <dd>{preview.tempo} BPM</dd>
            </div>
          </dl>
          <table className="parts">
            <thead>
              <tr>
                <th>Import</th>
                <th>Part</th>
                <th>Instrument</th>
                <th>Staves</th>
                <th>Notes</th>
                <th>Range</th>
              </tr>
            </thead>
            <tbody>
              {preview.parts.map((part) => (
                <tr key={part.id} aria-label={part.name}>
                  <td>
                    <input type="checkbox" aria-label={`Include ${part.name}`} checked={!excluded.has(part.id)} onChange={() => toggle(part.id)} />
                  </td>
                  <td>{part.name}</td>
                  <td>{part.instrument}</td>
                  <td>{part.staffCount}</td>
                  <td>{part.noteCount}</td>
                  <td>{part.lowest && part.highest ? `${part.lowest}–${part.highest}` : "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {preview.warnings.length > 0 && (
            <div className="warnings">
              <span className="muted">Import notes</span>
              <ul>
                {preview.warnings.map((warning) => (
                  <li key={warning}>{warning}</li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}
      <div className="actions">
        <button type="button" onClick={onCancel}>
          Cancel
        </button>
        <button type="button" className="primary" disabled={busy || included === 0} onClick={() => void importFile()}>
          Import {included} {included === 1 ? "part" : "parts"}
        </button>
      </div>
    </div>
  );
}
