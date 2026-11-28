import { describe, expect, it } from "vitest";
import { RpcClient, RpcError, type Transport } from "./rpcClient";

class FakeTransport implements Transport {
  readonly sent: string[] = [];
  private listener: (line: string) => void = () => {};

  send(line: string): void {
    this.sent.push(line);
  }

  onLine(listener: (line: string) => void): void {
    this.listener = listener;
  }

  receive(message: object): void {
    this.listener(JSON.stringify(message));
  }

  lastRequest(): { id: number; method: string; params: unknown } {
    return JSON.parse(this.sent[this.sent.length - 1]);
  }
}

describe("RpcClient", () => {
  it("sends a JSON-RPC request and resolves with the result of the same id", async () => {
    const transport = new FakeTransport();
    const client = new RpcClient(transport);

    const devices = client.request("devices.list", {});
    const request = transport.lastRequest();
    transport.receive({ jsonrpc: "2.0", id: request.id, result: { inputs: [], outputs: [] } });

    expect(request).toEqual({ jsonrpc: "2.0", id: request.id, method: "devices.list", params: {} });
    await expect(devices).resolves.toEqual({ inputs: [], outputs: [] });
  });

  it("matches responses to their requests by id", async () => {
    const transport = new FakeTransport();
    const client = new RpcClient(transport);

    const first = client.request("session.start", { input: "a", exercise: "random" });
    const firstId = transport.lastRequest().id;
    const second = client.request("settings.get", {});
    const secondId = transport.lastRequest().id;
    transport.receive({ jsonrpc: "2.0", id: secondId, result: { keyboards: {}, tempos: {}, latencyOffsetMs: 0, instrument: 0, keySound: false } });
    transport.receive({ jsonrpc: "2.0", id: firstId, result: { seed: 1, tempo: 90 } });

    await expect(first).resolves.toEqual({ seed: 1, tempo: 90 });
    await expect(second).resolves.toMatchObject({ latencyOffsetMs: 0, instrument: 0, keySound: false });
    expect(firstId).not.toBe(secondId);
  });

  it("rejects with the backend's error", async () => {
    const transport = new FakeTransport();
    const client = new RpcClient(transport);

    const started = client.request("session.start", { input: "x", exercise: "random" });
    transport.receive({
      jsonrpc: "2.0",
      id: transport.lastRequest().id,
      error: { code: -32002, message: "MIDI device unavailable: x" },
    });

    await expect(started).rejects.toEqual(new RpcError(-32002, "MIDI device unavailable: x"));
  });

  it("passes events to their listeners until they unsubscribe", () => {
    const transport = new FakeTransport();
    const client = new RpcClient(transport);
    const beats: boolean[] = [];

    const unsubscribe = client.on("beat", (beat) => beats.push(beat.accent));
    transport.receive({ jsonrpc: "2.0", method: "beat", params: { accent: true } });
    transport.receive({ jsonrpc: "2.0", method: "eval", params: { result: "WRONG" } });
    unsubscribe();
    transport.receive({ jsonrpc: "2.0", method: "beat", params: { accent: false } });

    expect(beats).toEqual([true]);
  });

  it("fails every pending request when the backend stops", async () => {
    const transport = new FakeTransport();
    const client = new RpcClient(transport);

    const pending = client.request("devices.list", {});
    client.disconnect("The backend stopped (exit code 1)");

    await expect(pending).rejects.toThrow("The backend stopped (exit code 1)");
    await expect(client.request("devices.list", {})).rejects.toThrow("The backend stopped");
  });

  it("ignores lines that are not JSON-RPC messages", () => {
    const transport = new FakeTransport();
    new RpcClient(transport);

    expect(() => transport.receive({ hello: "world" })).not.toThrow();
  });
});
