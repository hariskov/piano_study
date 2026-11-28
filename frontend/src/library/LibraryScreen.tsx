import { useCallback, useEffect, useState } from "react";
import { useBackend } from "../app/backendContext";
import { SheetMusic } from "../sheet/SheetMusic";
import type { PastSession, SongDetails, SongSummary } from "../api/protocol";
import { isMidiFile, MidiImport } from "./MidiImport";

interface Props {
  chooseFile: () => Promise<string | undefined>;
  onPractise: (song: SongDetails) => void;
}

export function LibraryScreen({ chooseFile, onPractise }: Props) {
  const client = useBackend();
  const [songs, setSongs] = useState<SongSummary[]>();
  const [search, setSearch] = useState("");
  const [details, setDetails] = useState<SongDetails>();
  const [imported, setImported] = useState<SongSummary>();
  const [problem, setProblem] = useState<string>();
  const [busy, setBusy] = useState(false);
  const [midiFile, setMidiFile] = useState<string>();

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
    setProblem(undefined);
    setImported(undefined);
    if (isMidiFile(path)) {
      setMidiFile(path);
      return;
    }
    setBusy(true);
    try {
      await showImported(await client.request("library.import", { path }));
    } catch (error) {
      setProblem((error as Error).message);
    } finally {
      setBusy(false);
    }
  };

  const showImported = async (song: SongSummary) => {
    setMidiFile(undefined);
    setImported(song);
    await refresh();
    await open(song.id);
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
        {midiFile ? (
          <MidiImport key={midiFile} path={midiFile} onImported={(song) => void showImported(song)} onCancel={() => setMidiFile(undefined)} />
        ) : details ? <SongView key={details.id} song={details} onDelete={() => void remove(details)} onPractise={() => onPractise(details)} /> : (
          <div className="card muted song-details">Choose a song to see its details.</div>
        )}
      </div>
    </section>
  );
}

function SongView({ song, onDelete, onPractise }: { song: SongDetails; onDelete: () => void; onPractise: () => void }) {
  const client = useBackend();
  const [xml, setXml] = useState<string>();
  const [problem, setProblem] = useState<string>();
  const [sessions, setSessions] = useState<PastSession[]>([]);
  useEffect(() => {
    void client.request("progress.history", { songId: song.id }).then(
      (result) => setSessions(result.sessions),
      () => setSessions([]),
    );
  }, [client, song.id]);
  const showSheet = () =>
    void client.request("library.musicXml", { id: song.id }).then(
      (result) => setXml(result.xml),
      (error: Error) => setProblem(error.message),
    );
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
        {xml === undefined && (
          <button type="button" onClick={showSheet}>
            Show sheet music
          </button>
        )}
        <button type="button" className="primary" onClick={onPractise}>
          Practise
        </button>
      </div>
      {sessions.length > 0 && <SessionHistory sessions={sessions} measureCount={song.measureCount} />}
      {problem && <p className="problem">{problem}</p>}
      {xml !== undefined && <SheetMusic xml={xml} />}
    </div>
  );
}

const SHOWN_SESSIONS = 10;
const HANDS: Record<string, string> = { right: "Right hand", left: "Left hand", both: "Both hands" };

function SessionHistory({ sessions, measureCount }: { sessions: readonly PastSession[]; measureCount: number }) {
  const recent = sessions.slice(-SHOWN_SESSIONS).reverse();
  return (
    <table className="parts history">
      <caption>Your sessions</caption>
      <thead>
        <tr>
          <th>Date</th>
          <th>Hands</th>
          <th>Section</th>
          <th>Tempo</th>
          <th>Accuracy</th>
        </tr>
      </thead>
      <tbody>
        {recent.map((session) => (
          <tr key={session.finishedAt}>
            <td>{new Date(session.finishedAt).toLocaleDateString()}</td>
            <td>{HANDS[session.hands] ?? session.hands}</td>
            <td>
              {session.fromBar === undefined || (session.fromBar === 1 && session.toBar === measureCount)
                ? "Whole song"
                : `Bars ${session.fromBar}–${session.toBar}`}
            </td>
            <td>{session.tempoPercent ?? 100}%</td>
            <td>
              <strong>{session.accuracyPercent}%</strong>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
