import { Icon } from "../icons/Icon";
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
          <Icon name="practice" />
          <span className="tile-title">Random notes and chords</span>
          <span className="tile-text">Read and play generated exercises, from single notes to both hands.</span>
        </button>
        <button type="button" className="tile" onClick={() => onOpen("devices")}>
          <Icon name="devices" />
          <span className="tile-title">Devices</span>
          <span className="tile-text">Choose your piano, the sound output and the keyboard size.</span>
        </button>
        <button type="button" className="tile" onClick={() => onOpen("library")}>
          <Icon name="songs" />
          <span className="tile-title">Songs</span>
          <span className="tile-text">Import MIDI and MusicXML files, find them by name and open them.</span>
        </button>
      </div>
    </section>
  );
}
