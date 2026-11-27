import { RpcClient } from "./rpcClient";

export interface BackendBridge {
  send(line: string): void;
  onLine(listener: (line: string) => void): void;
  onExit(listener: (code: number | null) => void): void;
}

declare global {
  interface Window {
    backend?: BackendBridge;
  }
}

export function connectToBackend(bridge: BackendBridge | undefined = window.backend): RpcClient | undefined {
  if (bridge === undefined) {
    return undefined;
  }
  const client = new RpcClient(bridge);
  bridge.onExit((code) => client.disconnect(`The backend stopped (exit code ${code ?? "unknown"})`));
  return client;
}
