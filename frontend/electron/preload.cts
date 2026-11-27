import { contextBridge, ipcRenderer } from "electron";

contextBridge.exposeInMainWorld("backend", {
  send: (line: string) => ipcRenderer.send("backend:line", line),
  onLine: (listener: (line: string) => void) => {
    ipcRenderer.on("backend:line", (_event, line: string) => listener(line));
  },
  onExit: (listener: (code: number | null) => void) => {
    ipcRenderer.on("backend:exit", (_event, code: number | null) => listener(code));
  },
});
