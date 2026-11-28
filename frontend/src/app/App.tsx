import { useEffect, useMemo, useState } from "react";
import { connectToBackend } from "../api/backend";
import type { SessionStartParams, SongDetails } from "../api/protocol";
import { DevicesScreen } from "../devices/DevicesScreen";
import { LibraryScreen } from "../library/LibraryScreen";
import { SongSetup } from "../library/SongSetup";
import { PracticeScreen } from "../practice/PracticeScreen";
import { SetupScreen } from "../practice/SetupScreen";
import type { Setup } from "../practice/setupForm";
import { BackendContext } from "./backendContext";
import { HomeScreen } from "./HomeScreen";

export type Screen = "home" | "library" | "devices" | "practice";

const NAVIGATION: readonly { screen: Screen; label: string }[] = [
  { screen: "home", label: "Home" },
  { screen: "practice", label: "Practice" },
  { screen: "library", label: "Library" },
  { screen: "devices", label: "Devices" },
];

const LOG_FILE = "~/.local/state/piano-player/backend.log";

export function App() {
  const client = useMemo(() => connectToBackend(), []);
  const [stopped, setStopped] = useState<string>();
  const [restarted, setRestarted] = useState(() => new URLSearchParams(window.location.search).has("restarted"));
  useEffect(() => client?.onDisconnect(setStopped), [client]);
  const [screen, setScreen] = useState<Screen>(screenFromHash);
  const [started, setStarted] = useState<SessionStartParams>();
  const [lastSetup, setLastSetup] = useState<Setup>();
  const [songToPractise, setSongToPractise] = useState<SongDetails>();
  const open = (next: Screen) => {
    window.location.hash = next;
    setScreen(next);
  };

  if (client === undefined) {
    return (
      <main className="app">
        <p role="alert" className="problem">
          Not running inside the Piano Player app, so there is no backend to talk to. Start it with bin/ui.
        </p>
      </main>
    );
  }

  return (
    <BackendContext.Provider value={client}>
      <div className="app">
        <header className="topbar">
          <span className="brand">Piano Player</span>
          <nav>
            {NAVIGATION.map((item) => (
              <button
                key={item.screen}
                type="button"
                className={item.screen === screen ? "nav active" : "nav"}
                onClick={() => {
                  setStarted(undefined);
                  setSongToPractise(undefined);
                  open(item.screen);
                }}
              >
                {item.label}
              </button>
            ))}
          </nav>
        </header>
        <main>
          {stopped && (
            <p role="alert" className="problem banner">
              {stopped}. Close Piano Player and start it again; the details are in {LOG_FILE}.
            </p>
          )}
          {restarted && !stopped && (
            <p role="status" className="notice banner">
              The music engine stopped unexpectedly and was restarted, so any session in progress was ended. The details
              are in {LOG_FILE}.{" "}
              <button type="button" onClick={() => setRestarted(false)}>
                OK
              </button>
            </p>
          )}
          {screen === "home" && <HomeScreen onOpen={open} />}
          {screen === "devices" && <DevicesScreen />}
          {screen === "library" && (
            <LibraryScreen
              chooseFile={() => window.backend?.chooseSongFile() ?? Promise.resolve(undefined)}
              onPractise={(song) => {
                setStarted(undefined);
                setSongToPractise(song);
                open("practice");
              }}
            />
          )}
          {screen === "practice" &&
            (started ? (
              <PracticeScreen
                params={started}
                setup={started.exercise === "song" ? undefined : lastSetup}
                onSetupChange={setLastSetup}
                onChangeSettings={() => setStarted(undefined)}
              />
            ) : songToPractise ? (
              <SongSetup song={songToPractise} onStart={setStarted} onBack={() => {
                setSongToPractise(undefined);
                open("library");
              }} />
            ) : (
              <SetupScreen
                previous={lastSetup}
                onStart={(params, setup) => {
                  setLastSetup(setup);
                  setStarted(params);
                }}
                onChooseDevice={() => open("devices")}
              />
            ))}
        </main>
      </div>
    </BackendContext.Provider>
  );
}

function screenFromHash(): Screen {
  const hash = window.location.hash.slice(1);
  return NAVIGATION.find((item) => item.screen === hash)?.screen ?? "home";
}
