import { describe, expect, it } from "vitest";
import { installWebKitBridge, type ShellWindow } from "./webkitBridge";

function shellWindow(): { target: ShellWindow; posted: unknown[] } {
  const posted: unknown[] = [];
  const target = {
    webkit: { messageHandlers: { shell: { postMessage: (message: string) => posted.push(JSON.parse(message)) } } },
  } as ShellWindow;
  return { target, posted };
}

describe("installWebKitBridge", () => {
  it("does nothing outside the Piano Player window", () => {
    const target = {} as ShellWindow;

    installWebKitBridge(target);

    expect(target.backend).toBeUndefined();
  });

  it("sends lines to the shell", () => {
    const { target, posted } = shellWindow();
    installWebKitBridge(target);

    target.backend!.send('{"id":1}');

    expect(posted).toEqual([{ type: "line", line: '{"id":1}' }]);
  });

  it("passes the backend's lines and exit to the listeners", () => {
    const { target } = shellWindow();
    installWebKitBridge(target);
    const lines: string[] = [];
    const exits: (number | null)[] = [];
    target.backend!.onLine((line) => lines.push(line));
    target.backend!.onExit((code) => exits.push(code));

    target.piano!.line("first");
    target.piano!.line("second");
    target.piano!.exit(3);

    expect(lines).toEqual(["first", "second"]);
    expect(exits).toEqual([3]);
  });

  it("asks the shell for a song file and answers with the chosen path", async () => {
    const { target, posted } = shellWindow();
    installWebKitBridge(target);

    const first = target.backend!.chooseSongFile();
    const second = target.backend!.chooseSongFile();
    target.piano!.chosen(1, null);
    target.piano!.chosen(0, "/home/me/song.mid");

    expect(posted).toEqual([{ type: "chooseSong", id: 0 }, { type: "chooseSong", id: 1 }]);
    await expect(first).resolves.toBe("/home/me/song.mid");
    await expect(second).resolves.toBeUndefined();
  });
});
