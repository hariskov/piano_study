import { useEffect, useReducer, useState } from "react";
import { useBackend } from "../app/backendContext";
import type { AdjustParams, ApiEvents, SessionStartParams } from "../api/protocol";
import { PianoKeyboard } from "../keyboard/PianoKeyboard";
import { keyboardRange, type KeyRange } from "../keyboard/presets";
import { pitchName } from "../music/pitch";
import { TempoControl } from "../tempo/TempoControl";
import { midiForKey } from "./computerKeys";
import { AccidentalsCheck, ChordLabelSelect, ChordTypeChecks, HandsSelect, LevelSelect, MixedCheck } from "./exerciseControls";
import { NoteLanes } from "./NoteLanes";
import { SheetMusic } from "../sheet/SheetMusic";
import { scorePosition } from "../sheet/scorePosition";
import { targetLabel, type ChordLabels } from "./chordLabel";
import { initialPractice, practiceReducer, type PracticeAction } from "./practiceState";
import { ResultsView } from "./ResultsView";
import type { Setup } from "./setupForm";

interface Props {
  params: SessionStartParams;
  setup?: Setup;
  onSetupChange?: (setup: Setup) => void;
  onChangeSettings: () => void;
}

const VIRTUAL_KEYBOARD = "virtual:keyboard";
const EVENTS: readonly (keyof ApiEvents)[] = [
  "session.state",
  "session.target",
  "eval",
  "input",
  "beat",
  "session.summary",
  "session.inputLost",
  "session.inputRestored",
];

export function PracticeScreen({ params, setup, onSetupChange, onChangeSettings }: Props) {
  const client = useBackend();
  const [run, setRun] = useState(0);
  const [practice, dispatch] = useReducer(
    (state: typeof initialPractice, action: PracticeAction | "reset") =>
      action === "reset" ? initialPractice : practiceReducer(state, action),
    initialPractice,
  );
  const [tempo, setTempo] = useState(params.tempo ?? 90);
  const [current, setCurrent] = useState(setup);
  const [songTempo, setSongTempo] = useState<number>();
  const [notices, setNotices] = useState<string[]>([]);
  const [section, setSection] = useState<{ start?: string; passLength?: string }>({});
  const [sheet, setSheet] = useState<string>();
  const [problem, setProblem] = useState<string>();
  const [range, setRange] = useState<KeyRange>(keyboardRange(undefined));
  const virtual = params.input === VIRTUAL_KEYBOARD;

  useEffect(() => {
    if (params.exercise === "song" && params.songId) {
      void client.request("library.musicXml", { id: params.songId }).then(
        (result) => setSheet(result.xml),
        () => setSheet(undefined),
      );
    }
  }, [client, params.exercise, params.songId]);

  useEffect(() => {
    void client.request("settings.get", {}).then((settings) => {
      try {
        setRange(keyboardRange(params.input ? settings.keyboards[params.input] : undefined));
      } catch {
        setRange(keyboardRange(undefined));
      }
    });
  }, [client, params.input]);

  useEffect(() => {
    dispatch("reset");
    setProblem(undefined);
    const unsubscribes = EVENTS.map((event) =>
      client.on(event, (eventParams) => dispatch({ event, params: eventParams } as PracticeAction)),
    );
    let finished = false;
    const stopOnSummary = client.on("session.summary", () => (finished = true));
    client.request("session.start", params).then(
      (started) => {
        setTempo(started.tempo);
        setSongTempo(started.songTempo);
        setNotices(started.notices);
        setSection({ start: started.sectionStart, passLength: started.passLength });
      },
      (error: Error) => {
        finished = true;
        setProblem(error.message);
      },
    );
    return () => {
      unsubscribes.forEach((unsubscribe) => unsubscribe());
      stopOnSummary();
      if (!finished) {
        void client.request("session.stop", {}).catch(() => undefined);
      }
    };
  }, [client, params, run]);

  useEffect(() => {
    if (!virtual) {
      return;
    }
    const isTyping = (event: KeyboardEvent) => event.target instanceof HTMLInputElement;
    const down = (event: KeyboardEvent) => {
      const midi = midiForKey(event.key);
      if (midi !== undefined && !event.repeat && !isTyping(event)) {
        void client.request("keyboard.press", { pitch: pitchName(midi) });
      }
    };
    const up = (event: KeyboardEvent) => {
      const midi = midiForKey(event.key);
      if (midi !== undefined && !isTyping(event)) {
        void client.request("keyboard.release", { pitch: pitchName(midi) });
      }
    };
    window.addEventListener("keydown", down);
    window.addEventListener("keyup", up);
    return () => {
      window.removeEventListener("keydown", down);
      window.removeEventListener("keyup", up);
    };
  }, [client, virtual]);

  if (problem) {
    return (
      <section className="screen">
        <p role="alert" className="problem">
          {problem}
        </p>
        <button type="button" onClick={onChangeSettings}>
          Change settings
        </button>
      </section>
    );
  }
  if (practice.summary) {
    return <ResultsView summary={practice.summary} onAgain={() => setRun(run + 1)} onChangeSettings={onChangeSettings} />;
  }

  const adjust = (change: Partial<Setup> & AdjustParams) => {
    if (current === undefined) {
      return;
    }
    const next = { ...current, ...change };
    setCurrent(next);
    onSetupChange?.(next);
    void client.request("session.adjust", change).catch((error: Error) => setProblem(error.message));
  };
  const showLabels = (chordLabels: ChordLabels) => {
    if (current !== undefined) {
      const next = { ...current, chordLabels };
      setCurrent(next);
      onSetupChange?.(next);
    }
  };
  const changeTempo = (bpm: number) => {
    setTempo(bpm);
    void client.request("session.changeTempo", { bpm }).catch(() => undefined);
  };
  const target = practice.target;
  const targets = new Set(target?.notes.map((note) => note.midi) ?? []);
  const nowPlaying = target ? targetLabel(target, current?.chordLabels ?? "both") : "";

  return (
    <section className="screen practice">
      <div className="practice-bar">
        <span key={practice.beat.count} className={practice.beat.accent ? "beat accent" : "beat"} aria-hidden="true" />
        <div className="practice-tempo">
          <TempoControl value={tempo} onChange={changeTempo} />
          {songTempo !== undefined && <span className="muted">{Math.round((tempo * 100) / songTempo)}% of the song</span>}
        </div>
        <div className="practice-buttons">
          {practice.state === "PAUSED" ? (
            <button type="button" className="primary" onClick={() => void client.request("session.resume", {})}>
              Resume
            </button>
          ) : (
            <button type="button" onClick={() => void client.request("session.pause", {})}>
              Pause
            </button>
          )}
          <button type="button" onClick={() => void client.request("session.stop", {})}>
            Stop
          </button>
        </div>
      </div>
      {notices.length > 0 && (
        <ul className="notices">
          {notices.map((notice) => (
            <li key={notice}>{notice}</li>
          ))}
        </ul>
      )}
      {sheet !== undefined && (
        <SheetMusic xml={sheet} position={target ? scorePosition(target.start, section.start, section.passLength) : undefined} />
      )}
      <div className="practice-main">
      <div className="stage card">
        <div className="now-playing" aria-label="Now playing">
          {nowPlaying || "…"}
        </div>
        {practice.lastGrade && (
          <div className={`grade ${practice.lastGrade.grade.toLowerCase()}`}>
            {practice.lastGrade.grade.toLowerCase()}{" "}
            <small>
              {practice.lastGrade.offsetMs === 0
                ? "on time"
                : `${Math.abs(practice.lastGrade.offsetMs)} ms ${practice.lastGrade.offsetMs < 0 ? "early" : "late"}`}
            </small>
          </div>
        )}
        <NoteLanes history={practice.history} target={target} />
        {practice.state === "COUNT_IN" && <div className="overlay">Get ready…</div>}
        {practice.state === "PAUSED" && <div className="overlay">Paused</div>}
        {practice.inputLost && (
          <div className="overlay problem">The piano was disconnected. Plug it back in, then resume.</div>
        )}
      </div>
      {current && params.exercise !== "song" && (
      <aside className="card adjust" aria-label="Adjust while playing">
        <h3>Adjust while playing</h3>
        <HandsSelect value={current.hands} onChange={(hands) => adjust({ hands })} />
        {current.exercise === "random" ? (
          <>
            <LevelSelect value={current.level} onChange={(level) => adjust({ level })} />
            <AccidentalsCheck checked={current.accidentals} onChange={(accidentals) => adjust({ accidentals })} />
            <MixedCheck checked={current.mixed} onChange={(mixed) => adjust({ mixed })} />
          </>
        ) : (
          <>
            <ChordTypeChecks value={current.chordTypes} onChange={(chordTypes) => adjust({ chordTypes })} />
            <ChordLabelSelect value={current.chordLabels} onChange={(chordLabels) => showLabels(chordLabels)} />
          </>
        )}
        <p className="muted small">Changes apply from the next bar.</p>
      </aside>
      )}
      </div>
      <div className="card keyboard-card">
        <PianoKeyboard
          low={range.low}
          high={range.high}
          pressed={practice.pressed}
          targets={targets}
          marks={practice.marks}
          onPress={virtual ? (midi) => void client.request("keyboard.press", { pitch: pitchName(midi) }) : undefined}
          onRelease={virtual ? (midi) => void client.request("keyboard.release", { pitch: pitchName(midi) }) : undefined}
        />
        {virtual && <p className="muted hint">Computer keys: A S D F G H J K L ; play C4–E5, W E T Y U O P the black keys.</p>}
      </div>
    </section>
  );
}

