import { describe, expect, it } from "vitest";
import { connectToBackend, type BackendBridge } from "./backend";

describe("connectToBackend", () => {
  it("has nothing to connect to outside the app window", () => {
    expect(connectToBackend(undefined)).toBeUndefined();
  });

  it("fails requests once the backend exits", async () => {
    let exit: (code: number | null) => void = () => {};
    const bridge: BackendBridge = {
      send: () => {},
      onLine: () => {},
      onExit: (listener) => (exit = listener),
      chooseSongFile: async () => undefined,
    };
    const client = connectToBackend(bridge)!;

    const pending = client.request("devices.list", {});
    exit(1);

    await expect(pending).rejects.toThrow("The backend stopped (exit code 1)");
  });
});
