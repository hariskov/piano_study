type Test = { name: string; run: () => Promise<void> };

const tests: Test[] = [];
const TIMEOUT_MS = 90_000;

interface ShellHandler {
  postMessage(message: string): void;
}

function post(message: object): void {
  const shell = (window as { webkit?: { messageHandlers?: { shell?: ShellHandler } } }).webkit?.messageHandlers?.shell;
  shell?.postMessage(JSON.stringify(message));
}

window.addEventListener("error", (event) => post({ type: "e2e.log", text: `page error: ${event.message}` }));

export function test(name: string, run: () => Promise<void>): void {
  tests.push({ name, run });
}

export async function screenshot(name: string): Promise<void> {
  post({ type: "e2e.screenshot", name });
  await new Promise((resolve) => setTimeout(resolve, 300));
}

function withTimeout(run: () => Promise<void>): Promise<void> {
  return Promise.race([
    run(),
    new Promise<void>((_, reject) => setTimeout(() => reject(new Error(`timed out after ${TIMEOUT_MS} ms`)), TIMEOUT_MS)),
  ]);
}

// Runs the tests in order, as one user would, and stops at the first failure since later ones depend on it.
export async function runAll(): Promise<void> {
  for (const { name, run } of tests) {
    try {
      await withTimeout(run);
      post({ type: "e2e.log", text: `  ✓ ${name}` });
    } catch (error) {
      post({ type: "e2e.log", text: `  ✗ ${name}\n    ${(error instanceof Error ? error.message : String(error)).split("\n")[0]}` });
      post({ type: "e2e.done", failed: true });
      return;
    }
  }
  post({ type: "e2e.log", text: `${tests.length} passed` });
  post({ type: "e2e.done", failed: false });
}
