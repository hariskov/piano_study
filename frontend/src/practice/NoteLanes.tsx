import type { TargetEvent } from "../api/protocol";
import type { PlayedTarget } from "./practiceState";

interface Props {
  history: readonly PlayedTarget[];
  target?: TargetEvent;
}

const HANDS = [
  { hand: "RIGHT", label: "Right hand" },
  { hand: "LEFT", label: "Left hand" },
] as const;

function notesFor(target: TargetEvent, hand: string): string {
  return target.notes
    .filter((note) => note.hand === hand)
    .map((note) => note.pitch)
    .join(" ");
}

export function NoteLanes({ history, target }: Props) {
  const usedHands = HANDS.filter(({ hand }) =>
    [...history.map((played) => played.target), ...(target ? [target] : [])].some((item) =>
      item.notes.some((note) => note.hand === hand),
    ),
  );
  const lanes = usedHands.length > 0 ? usedHands : HANDS.slice(0, 1);

  return (
    <div className="lanes">
      {lanes.map(({ hand, label }) => (
        <div key={hand} className="lane">
          <span className="lane-label">{label}</span>
          <div className="lane-track">
            {history.map((played, index) => (
              <span key={`${played.target.start}-${index}`} className={`block ${played.result}`}>
                {notesFor(played.target, hand)}
              </span>
            ))}
            <span className="now-line" />
            {target && <span className="block current">{notesFor(target, hand)}</span>}
          </div>
        </div>
      ))}
    </div>
  );
}
