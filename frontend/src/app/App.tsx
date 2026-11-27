import { useMemo, useState } from "react";
import { connectToBackend } from "../api/backend";
import { DevicesScreen } from "../devices/DevicesScreen";
import { BackendContext } from "./backendContext";
import { HomeScreen } from "./HomeScreen";

export type Screen = "home" | "devices" | "practice";

const NAVIGATION: readonly { screen: Screen; label: string }[] = [
  { screen: "home", label: "Home" },
  { screen: "practice", label: "Practice" },
  { screen: "devices", label: "Devices" },
];

export function App() {
  const client = useMemo(() => connectToBackend(), []);
  const [screen, setScreen] = useState<Screen>(screenFromHash);
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
                onClick={() => open(item.screen)}
              >
                {item.label}
              </button>
            ))}
          </nav>
        </header>
        <main>
          {screen === "home" && <HomeScreen onOpen={open} />}
          {screen === "devices" && <DevicesScreen />}
          {screen === "practice" && <p className="muted">Practice setup comes next.</p>}
        </main>
      </div>
    </BackendContext.Provider>
  );
}

function screenFromHash(): Screen {
  const hash = window.location.hash.slice(1);
  return NAVIGATION.find((item) => item.screen === hash)?.screen ?? "home";
}
