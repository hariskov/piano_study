import type { BackendBridge } from "./backend";

interface ShellReceiver {
  line(line: string): void;
  exit(code: number | null): void;
  chosen(id: number, path: string | null): void;
}

type ShellMessage = { type: "line"; line: string } | { type: "chooseSong"; id: number };

export interface ShellWindow {
  webkit?: { messageHandlers?: { shell?: { postMessage(message: string): void } } };
  backend?: BackendBridge;
  piano?: ShellReceiver;
}

declare global {
  interface Window {
    piano?: ShellReceiver;
  }
}

// The window shell (shell/piano-player.js) posts to the "shell" handler and calls window.piano.
export function installWebKitBridge(target: ShellWindow = window as ShellWindow): void {
  const handler = target.webkit?.messageHandlers?.shell;
  if (handler === undefined) {
    return;
  }
  const post = (message: ShellMessage) => handler.postMessage(JSON.stringify(message));
  const lineListeners: ((line: string) => void)[] = [];
  const exitListeners: ((code: number | null) => void)[] = [];
  const choices = new Map<number, (path: string | undefined) => void>();
  let nextChoice = 0;

  target.piano = {
    line: (line) => lineListeners.forEach((listener) => listener(line)),
    exit: (code) => exitListeners.forEach((listener) => listener(code)),
    chosen: (id, path) => {
      choices.get(id)?.(path ?? undefined);
      choices.delete(id);
    },
  };
  target.backend = {
    send: (line) => post({ type: "line", line }),
    onLine: (listener) => lineListeners.push(listener),
    onExit: (listener) => exitListeners.push(listener),
    chooseSongFile: () =>
      new Promise((resolve) => {
        const id = nextChoice++;
        choices.set(id, resolve);
        post({ type: "chooseSong", id });
      }),
  };
}
