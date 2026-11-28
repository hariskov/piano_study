import type { ApiEvents, ApiMethods } from "./protocol";

export interface Transport {
  send(line: string): void;
  onLine(listener: (line: string) => void): void;
}

export class RpcError extends Error {
  constructor(
    readonly code: number,
    message: string,
  ) {
    super(message);
    this.name = "RpcError";
  }
}

interface Pending {
  resolve(result: unknown): void;
  reject(error: Error): void;
}

type Listener = (params: never) => void;

export class RpcClient {
  private nextId = 1;
  private readonly pending = new Map<number, Pending>();
  private readonly listeners = new Map<string, Set<Listener>>();
  private stopped: string | undefined;
  private readonly disconnectListeners = new Set<(reason: string) => void>();

  constructor(private readonly transport: Transport) {
    transport.onLine((line) => this.receive(line));
  }

  request<M extends keyof ApiMethods>(
    method: M,
    params: ApiMethods[M]["params"],
  ): Promise<ApiMethods[M]["result"]> {
    if (this.stopped !== undefined) {
      return Promise.reject(new Error(this.stopped));
    }
    const id = this.nextId++;
    return new Promise((resolve, reject) => {
      this.pending.set(id, { resolve: resolve as (result: unknown) => void, reject });
      this.transport.send(JSON.stringify({ jsonrpc: "2.0", id, method, params }));
    });
  }

  on<E extends keyof ApiEvents>(event: E, listener: (params: ApiEvents[E]) => void): () => void {
    const forEvent = this.listeners.get(event) ?? new Set<Listener>();
    forEvent.add(listener as Listener);
    this.listeners.set(event, forEvent);
    return () => forEvent.delete(listener as Listener);
  }

  onDisconnect(listener: (reason: string) => void): () => void {
    this.disconnectListeners.add(listener);
    return () => this.disconnectListeners.delete(listener);
  }

  disconnect(reason: string): void {
    this.stopped = reason;
    this.disconnectListeners.forEach((listener) => listener(reason));
    for (const waiting of this.pending.values()) {
      waiting.reject(new Error(reason));
    }
    this.pending.clear();
  }

  private receive(line: string): void {
    const message = JSON.parse(line);
    if (typeof message.id === "number" && this.pending.has(message.id)) {
      const waiting = this.pending.get(message.id)!;
      this.pending.delete(message.id);
      if (message.error) {
        waiting.reject(new RpcError(message.error.code, message.error.message));
      } else {
        waiting.resolve(message.result);
      }
    } else if (typeof message.method === "string") {
      this.listeners.get(message.method)?.forEach((listener) => (listener as (params: unknown) => void)(message.params));
    }
  }
}
