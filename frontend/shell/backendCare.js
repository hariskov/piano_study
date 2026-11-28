// Plain JavaScript shared by the GJS window shell and its Vitest tests.

const RESTART_LIMIT = 3;
const RESTART_WINDOW_MS = 60_000;

// stops: the times the backend stopped unexpectedly, the latest included.
export function shouldRestart(stops, now) {
  return stops.filter((time) => now - time < RESTART_WINDOW_MS).length <= RESTART_LIMIT;
}

export function logFile(getenv, home) {
  const state = getenv("XDG_STATE_HOME") ?? `${home}/.local/state`;
  return `${state}/piano-player/backend.log`;
}
