import type { Screen } from "./App";

interface Props {
  onOpen: (screen: Screen) => void;
}

export function HomeScreen({ onOpen }: Props) {
  return (
    <section className="screen home">
      <h1>What would you like to practise?</h1>
      <div className="tiles">
        <button type="button" className="tile" onClick={() => onOpen("practice")}>
          <span className="tile-title">Random notes and chords</span>
          <span className="tile-text">Read and play generated exercises, from single notes to both hands.</span>
        </button>
        <button type="button" className="tile" onClick={() => onOpen("devices")}>
          <span className="tile-title">Devices</span>
          <span className="tile-text">Choose your piano, the sound output and the keyboard size.</span>
        </button>
        <div className="tile disabled" aria-disabled="true">
          <span className="tile-title">Songs</span>
          <span className="tile-text">MusicXML and MIDI songs arrive in version 0.8.</span>
        </div>
      </div>
    </section>
  );
}
