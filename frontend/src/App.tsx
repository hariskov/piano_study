import { useEffect, useState } from "react";
import { connectToBackend } from "./api/backend";
import type { DeviceList } from "./api/protocol";

export function App() {
  const [devices, setDevices] = useState<DeviceList>();
  const [problem, setProblem] = useState<string>();

  useEffect(() => {
    const client = connectToBackend();
    if (client === undefined) {
      setProblem("Not running inside the Piano Player app, so there is no backend to talk to.");
      return;
    }
    client.request("devices.list", {}).then(setDevices, (error: Error) => setProblem(error.message));
  }, []);

  return (
    <main>
      <h1>Piano Player</h1>
      {problem && <p role="alert">{problem}</p>}
      {devices && (
        <ul>
          {devices.inputs.map((device) => (
            <li key={device.id}>{device.name}</li>
          ))}
        </ul>
      )}
    </main>
  );
}
