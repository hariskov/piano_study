import { useCallback, useEffect, useState } from "react";
import { useBackend } from "../app/backendContext";
import type { ImportParams, SongDetails, SongSummary } from "../api/protocol";

interface Props {
  chooseFile: () => Promise<string | undefined>;
  onPractise: (song: SongDetails) => void;
}

interface MidiOptions {
  grid: number;
  triplets: boolean;
  splitPoint: string;
}

const DEFAULT_MIDI: MidiOptions = { grid: 16, triplets: false, splitPoint: "C4" };

export function LibraryScreen({ chooseFile, onPractise }: Props) {
  const client = useBackend();
  const [songs, setSongs] = useState<SongSummary[]>();
  const [search, setSearch] = useState("");
  const [details, setDetails] = useState<SongDetails>();
  const [imported, setImported] = useState<SongSummary>();
  const [problem, setProblem] = useState<string>();
  const [busy, setBusy] = useState(false);
  const [midi, setMidi] = useState<MidiOptions>(DEFAULT_MIDI);
  const [midiChanged, setMidiChanged] = useState(false);

  const refresh = useCallback(
    () => client.request("library.list", {}).then((list) => setSongs(list.songs)),
    [client],
  );
  useEffect(() => {
    void refresh();
  }, [refresh]);

  const open = async (id: string) => {
    setProblem(undefined);
    setDetails(await client.request("library.open", { id }));
  };

  const importSong = async () => {
    const path = await chooseFile();
    if (path === undefined) {
      return;
    }
    setBusy(true);
    setProblem(undefined);
    setImported(undefined);
    try {
      const params: ImportParams = midiChanged ? { path, ...midi } : { path };
      const song = await client.request("library.import", params);
      setImported(song);
      await refresh();
      await open(song.id);
    } catch (error) {
      setProblem((error as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const remove = async (song: SongDetails) => {
    if (!window.confirm(`Delete "${song.title}"? Its MusicXML and MIDI files will be removed.`)) {
      return;
    }
    await client.request("library.delete", { id: song.id });
    setDetails(undefined);
    setImported(undefined);
    await refresh();
  };

  const changeMidi = (change: Partial<MidiOptions>) => {
    setMidi({ ...midi, ...change });
    setMidiChanged(true);
  };

  const shown = (songs ?? []).filter((song) => song.title.toLowerCase().includes(search.trim().toLowerCase()));

  return (
    <section className="screen library">
      <div className="library-head">
        <h2>Library</h2>
        <input
          type="search"
          aria-label="Search songs"
          placeholder="Search by name…"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <button type="button" className="primary" disabled={busy} onClick={() => void importSong()}>
          Import…
        </button>
      </div>
      <details className="midi-options">
        <summary>MIDI options</summary>
        <label className="field inline">
          <span>Snap notes to</span>
          <select aria-label="Grid" value={midi.grid} onChange={(event) => changeMidi({ grid: Number(event.target.value) })}>
            {[4, 8, 16, 32].map((grid) => (
              <option key={grid} value={grid}>
                1/{grid}
              </option>
            ))}
          </select>
        </label>
        <label className="check">
          <input type="checkbox" aria-label="Triplets" checked={midi.triplets} onChange={(event) => changeMidi({ triplets: event.target.checked })} />
          Triplets
        </label>
        <label className="field inline">
          <span>Split hands at</span>
          <input aria-label="Split hands at" value={midi.splitPoint} onChange={(event) => changeMidi({ splitPoint: event.target.value })} />
        </label>
      </details>
      {problem && (
        <p role="alert" className="problem">
          {problem}
        </p>
      )}
      {imported && (
        <div className="card notice">
          <strong>Imported "{imported.title}"</strong> as {imported.fileName}
          {imported.warnings.length > 0 && (
            <ul>
              {imported.warnings.map((warning) => (
                <li key={warning}>{warning}</li>
              ))}
            </ul>
          )}
        </div>
      )}
      <div className="library-body">
        <ul className="card song-list" aria-label="Songs">
          {songs !== undefined && songs.length === 0 && (
            <li className="muted">No songs yet. Import a MIDI or MusicXML file to start.</li>
          )}
          {shown.map((song) => (
            <li key={song.id}>
              <button
                type="button"
                className={details?.id === song.id ? "song active" : "song"}
                onClick={() => void open(song.id)}
              >
                <span className="song-title">{song.title}</span>
                <span className="song-meta">
                  {song.format} · {song.partCount} {song.partCount === 1 ? "part" : "parts"} · {song.measureCount}{" "}
                  {song.measureCount === 1 ? "bar" : "bars"}
                </span>
              </button>
            </li>
          ))}
        </ul>
        {details ? <SongView song={details} onDelete={() => void remove(details)} onPractise={() => onPractise(details)} /> : (
          <div className="card muted song-details">Choose a song to see its details.</div>
        )}
      </div>
    </section>
  );
}

function SongView({ song, onDelete, onPractise }: { song: SongDetails; onDelete: () => void; onPractise: () => void }) {
  return (
    <div className="card song-details">
      <h3>{song.title}</h3>
      {song.composer && <p className="muted">{song.composer}</p>}
      <dl className="facts">
        <div>
          <dt>Bars</dt>
          <dd>{song.measureCount}</dd>
        </div>
        <div>
          <dt>Time</dt>
          <dd>{song.timeSignature}</dd>
        </div>
        <div>
          <dt>Key</dt>
          <dd>{song.key}</dd>
        </div>
        <div>
          <dt>Tempo</dt>
          <dd>{song.tempo} BPM</dd>
        </div>
      </dl>
      <table className="parts">
        <thead>
          <tr>
            <th>Part</th>
            <th>Staves</th>
            <th>Notes</th>
            <th>Range</th>
          </tr>
        </thead>
        <tbody>
          {song.parts.map((part) => (
            <tr key={part.id}>
              <td>{part.name}</td>
              <td>{part.staffCount}</td>
              <td>{part.noteCount}</td>
              <td>{part.lowest && part.highest ? `${part.lowest}–${part.highest}` : "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="file">
        <span className="muted">MusicXML file</span>
        <code>{song.file}</code>
      </p>
      {song.midiFile && (
        <p className="file">
          <span className="muted">Original MIDI</span>
          <code>{song.midiFile}</code>
        </p>
      )}
      {song.warnings.length > 0 && (
        <div className="warnings">
          <span className="muted">Import notes</span>
          <ul>
            {song.warnings.map((warning) => (
              <li key={warning}>{warning}</li>
            ))}
          </ul>
        </div>
      )}
      <div className="actions">
        <button type="button" onClick={onDelete}>
          Delete
        </button>
        <button type="button" className="primary" onClick={onPractise}>
          Practise
        </button>
      </div>
    </div>
  );
}
