import { RpcClient, RpcError, type Transport } from "../api/rpcClient";
import type { ApiEvents, ApiMethods } from "../api/protocol";

type Handler<M extends keyof ApiMethods> = (params: ApiMethods[M]["params"]) => ApiMethods[M]["result"];

export class FakeBackend implements Transport {
  readonly client: RpcClient;
  readonly calls: { method: string; params: unknown }[] = [];
  private readonly handlers = new Map<string, (params: unknown) => unknown>();
  private listener: (line: string) => void = () => {};

  constructor() {
    this.client = new RpcClient(this);
  }

  answer<M extends keyof ApiMethods>(method: M, handler: Handler<M>): this {
    this.handlers.set(method, handler as (params: unknown) => unknown);
    return this;
  }

  fail(method: keyof ApiMethods, code: number, message: string): this {
    this.handlers.set(method, () => {
      throw new RpcError(code, message);
    });
    return this;
  }

  emit<E extends keyof ApiEvents>(event: E, params: ApiEvents[E]): void {
    this.listener(JSON.stringify({ jsonrpc: "2.0", method: event, params }));
  }

  callsOf(method: keyof ApiMethods): unknown[] {
    return this.calls.filter((call) => call.method === method).map((call) => call.params);
  }

  send(line: string): void {
    const request = JSON.parse(line);
    this.calls.push({ method: request.method, params: request.params });
    const handler = this.handlers.get(request.method) ?? (() => null);
    queueMicrotask(() => {
      try {
        this.listener(JSON.stringify({ jsonrpc: "2.0", id: request.id, result: handler(request.params) }));
      } catch (error) {
        const failure = error as RpcError;
        this.listener(JSON.stringify({ jsonrpc: "2.0", id: request.id, error: { code: failure.code, message: failure.message } }));
      }
    });
  }

  onLine(listener: (line: string) => void): void {
    this.listener = listener;
  }
}
