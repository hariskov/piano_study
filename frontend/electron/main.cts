import { app, BrowserWindow, dialog, ipcMain } from "electron";
import { spawn, type ChildProcessWithoutNullStreams } from "node:child_process";
import path from "node:path";
import { createInterface } from "node:readline";
import { backendCommand } from "./backendCommand.cjs";

let backend: ChildProcessWithoutNullStreams | undefined;

function startBackend(window: BrowserWindow): ChildProcessWithoutNullStreams {
  const command = backendCommand({
    env: process.env,
    isPackaged: app.isPackaged,
    resourcesPath: process.resourcesPath,
    appPath: app.getAppPath(),
  });
  const child = spawn(command, ["api"], { stdio: ["pipe", "pipe", "pipe"] });
  createInterface({ input: child.stdout }).on("line", (line) => window.webContents.send("backend:line", line));
  child.stderr.on("data", (chunk: Buffer) => process.stderr.write(chunk));
  child.on("exit", (code) => {
    if (!window.isDestroyed()) {
      window.webContents.send("backend:exit", code);
    }
  });
  child.on("error", (error) => {
    process.stderr.write(`Could not start the backend (${command}): ${error.message}\n`);
    if (!window.isDestroyed()) {
      window.webContents.send("backend:exit", null);
    }
  });
  return child;
}

function createWindow(): void {
  const window = new BrowserWindow({
    width: 1280,
    height: 800,
    title: "Piano Player",
    autoHideMenuBar: true,
    backgroundColor: "#14161b",
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
      contextIsolation: true,
      sandbox: true,
      nodeIntegration: false,
    },
  });
  backend = startBackend(window);
  const devServer = process.env.VITE_DEV_SERVER_URL;
  if (devServer) {
    void window.loadURL(devServer);
  } else {
    void window.loadFile(path.join(__dirname, "..", "renderer", "index.html"), { hash: process.env.PP_SCREEN ?? "" });
  }
}

ipcMain.on("backend:line", (_event, line: string) => {
  backend?.stdin.write(line + "\n");
});

ipcMain.handle("dialog:chooseSong", async (event) => {
  const owner = BrowserWindow.fromWebContents(event.sender);
  const options = {
    title: "Import a song",
    properties: ["openFile" as const],
    filters: [
      { name: "MIDI and MusicXML", extensions: ["mid", "midi", "musicxml", "xml", "mxl"] },
      { name: "All files", extensions: ["*"] },
    ],
  };
  const chosen = owner ? await dialog.showOpenDialog(owner, options) : await dialog.showOpenDialog(options);
  return chosen.canceled ? undefined : chosen.filePaths[0];
});

app.whenReady().then(createWindow);

app.on("window-all-closed", () => {
  // Closing stdin is the backend's signal to stop its session, close the MIDI devices and exit.
  backend?.stdin.end();
  app.quit();
});
