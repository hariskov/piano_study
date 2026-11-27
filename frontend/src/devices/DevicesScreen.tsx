import { useEffect, useState } from "react";
import { useBackend } from "../app/backendContext";
import type { DeviceList, SettingsView } from "../api/protocol";
import { PianoKeyboard } from "../keyboard/PianoKeyboard";
import { KEYBOARD_PRESETS, keyboardRange } from "../keyboard/presets";
import { pitchName } from "../music/pitch";

const VIRTUAL_KEYBOARD = "virtual:keyboard";
const CUSTOM = "custom";

export function DevicesScreen() {
  const client = useBackend();
  const [devices, setDevices] = useState<DeviceList>();
  const [settings, setSettings] = useState<SettingsView>();
  const [input, setInput] = useState<string>();
  const [customRange, setCustomRange] = useState("");
  const [rangeProblem, setRangeProblem] = useState<string>();
  const [pressed, setPressed] = useState<ReadonlySet<number>>(new Set());

  useEffect(() => {
    void Promise.all([client.request("devices.list", {}), client.request("settings.get", {})]).then(([list, saved]) => {
      setDevices(list);
      setSettings(saved);
      const shown = saved.input ?? list.inputs[0]?.id;
      setInput(shown);
      if (saved.input === undefined && shown !== undefined) {
        void client.request("settings.update", { input: shown }).then(setSettings);
      }
    });
    return client.on("devices.changed", setDevices);
  }, [client]);

  useEffect(() => {
    if (input === undefined) {
      return;
    }
    setPressed(new Set());
    void client.request("devices.monitor", { input });
    const unsubscribe = client.on("input", (event) => {
      if (event.midi === undefined) {
        return;
      }
      const midi = event.midi;
      setPressed((current) => {
        const next = new Set(current);
        if (event.type === "NOTE_ON") {
          next.add(midi);
        } else {
          next.delete(midi);
        }
        return next;
      });
    });
    return () => {
      unsubscribe();
      void client.request("devices.stopMonitor", {}).catch(() => undefined);
    };
  }, [client, input]);

  const saved = input ? settings?.keyboards[input] : undefined;
  const isPreset = saved === undefined || KEYBOARD_PRESETS.some((preset) => String(preset.keys) === saved);
  const range = safeRange(saved);

  const save = async (update: Parameters<typeof client.request<"settings.update">>[1]) =>
    setSettings(await client.request("settings.update", update));

  const chooseInput = (id: string) => {
    setInput(id);
    void save({ input: id });
  };

  const chooseKeyboard = (value: string) => {
    if (input === undefined) {
      return;
    }
    if (value === CUSTOM) {
      setCustomRange(saved && !isPreset ? saved : "");
      setSettings((current) => current && { ...current, keyboards: { ...current.keyboards, [input]: CUSTOM } });
      return;
    }
    void save({ keyboards: { [input]: value } });
  };

  const applyCustomRange = () => {
    if (input === undefined) {
      return;
    }
    try {
      keyboardRange(customRange);
      setRangeProblem(undefined);
      void save({ keyboards: { [input]: customRange } });
    } catch {
      setRangeProblem("Enter a range like F1-F7");
    }
  };

  if (!devices || !settings) {
    return <p className="muted">Looking for MIDI devices…</p>;
  }

  return (
    <section className="screen devices">
      <h2>Devices</h2>
      <div className="columns">
        <fieldset className="card">
          <legend>Piano (input)</legend>
          {devices.inputs.length === 0 && <p className="muted">No MIDI input found. Plug in the piano.</p>}
          {devices.inputs.map((device) => (
            <label key={device.id} className="choice">
              <input
                type="radio"
                name="input"
                checked={device.id === input}
                onChange={() => chooseInput(device.id)}
                aria-label={device.name}
              />
              <span>
                <strong>{device.name}</strong>
                <small>{device.description}</small>
              </span>
            </label>
          ))}
        </fieldset>
        <div className="card">
          <label className="field">
            <span>Sound and metronome (output)</span>
            <select value={settings.output ?? ""} onChange={(event) => void save({ output: event.target.value })}>
              <option value="" disabled>
                Choose an output
              </option>
              {devices.outputs.map((device) => (
                <option key={device.id} value={device.id}>
                  {device.name}
                </option>
              ))}
            </select>
          </label>
          <label className="field">
            <span>Keyboard size</span>
            <select
              aria-label="Keyboard size"
              value={isPreset ? (saved ?? "88") : CUSTOM}
              onChange={(event) => chooseKeyboard(event.target.value)}
            >
              {KEYBOARD_PRESETS.map((preset) => (
                <option key={preset.keys} value={String(preset.keys)}>
                  {preset.keys} keys ({preset.lowest}–{preset.highest})
                </option>
              ))}
              <option value={CUSTOM}>Custom range…</option>
            </select>
          </label>
          {!isPreset && (
            <div className="field inline">
              <input
                aria-label="Custom range"
                placeholder="F1-F7"
                value={customRange}
                onChange={(event) => setCustomRange(event.target.value)}
                onKeyDown={(event) => event.key === "Enter" && applyCustomRange()}
              />
              <button type="button" onClick={applyCustomRange}>
                Use range
              </button>
              {rangeProblem && <span className="problem">{rangeProblem}</span>}
            </div>
          )}
        </div>
      </div>
      <div className="card keyboard-card">
        <p className="muted">
          {input === VIRTUAL_KEYBOARD
            ? "Click the keys to play the virtual keyboard."
            : "Press some keys on your piano: they light up here."}
        </p>
        <PianoKeyboard
          low={range.low}
          high={range.high}
          pressed={pressed}
          onPress={input === VIRTUAL_KEYBOARD ? (midi) => void press(client, midi) : undefined}
          onRelease={input === VIRTUAL_KEYBOARD ? (midi) => void release(client, midi) : undefined}
        />
      </div>
    </section>
  );
}

function safeRange(saved: string | undefined) {
  try {
    return keyboardRange(saved === CUSTOM ? undefined : saved);
  } catch {
    return keyboardRange(undefined);
  }
}

function press(client: ReturnType<typeof useBackend>, midi: number) {
  return client.request("keyboard.press", { pitch: pitchName(midi) });
}

function release(client: ReturnType<typeof useBackend>, midi: number) {
  return client.request("keyboard.release", { pitch: pitchName(midi) });
}
