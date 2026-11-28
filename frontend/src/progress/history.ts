import type { PastSession } from "../api/protocol";

const SHOWN_BARS = 3;

export function previousOf(sessions: readonly PastSession[]): PastSession | undefined {
  return sessions.length >= 2 ? sessions[sessions.length - 2] : undefined;
}

export function troubleBars(session: PastSession): number[] {
  return Object.entries(session.mistakesByBar)
    .map(([bar, mistakes]) => ({ bar: Number(bar), mistakes }))
    .sort((a, b) => b.mistakes - a.mistakes || a.bar - b.bar)
    .slice(0, SHOWN_BARS)
    .map((entry) => entry.bar);
}
