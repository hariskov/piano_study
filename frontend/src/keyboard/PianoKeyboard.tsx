import { layoutKeys, BLACK_KEY_WIDTH } from "./layout";

export type KeyMark = "correct" | "wrong";

interface Props {
  low: number;
  high: number;
  pressed?: ReadonlySet<number>;
  targets?: ReadonlySet<number>;
  marks?: ReadonlyMap<number, KeyMark>;
  onPress?: (midi: number) => void;
  onRelease?: (midi: number) => void;
}

const WHITE_WIDTH = 24;
const WHITE_HEIGHT = 120;
const BLACK_HEIGHT = 76;
const NO_KEYS: ReadonlySet<number> = new Set();

export function PianoKeyboard({ low, high, pressed = NO_KEYS, targets = NO_KEYS, marks, onPress, onRelease }: Props) {
  const layout = layoutKeys(low, high);
  const classesOf = (midi: number, black: boolean) =>
    [
      "key",
      black ? "black" : "white",
      pressed.has(midi) ? "pressed" : "",
      targets.has(midi) ? "target" : "",
      marks?.get(midi) ?? "",
    ]
      .filter(Boolean)
      .join(" ");
  const handlers = (midi: number) => ({
    onPointerDown: () => onPress?.(midi),
    onPointerUp: () => onRelease?.(midi),
    onPointerLeave: (event: React.PointerEvent) => {
      if (event.buttons > 0) {
        onRelease?.(midi);
      }
    },
  });
  const whites = layout.keys.filter((key) => !key.black);
  const blacks = layout.keys.filter((key) => key.black);

  return (
    <svg
      className={onPress ? "piano-keyboard playable" : "piano-keyboard"}
      viewBox={`0 0 ${layout.whiteKeyCount * WHITE_WIDTH} ${WHITE_HEIGHT}`}
      preserveAspectRatio="xMidYMid meet"
      role="img"
      aria-label={`Keyboard ${layout.keys[0]?.name}–${layout.keys[layout.keys.length - 1]?.name}`}
    >
      {whites.map((key) => (
        <g key={key.midi}>
          <rect
            data-midi={key.midi}
            className={classesOf(key.midi, false)}
            x={key.x * WHITE_WIDTH}
            y={0}
            width={WHITE_WIDTH}
            height={WHITE_HEIGHT}
            rx={3}
            {...handlers(key.midi)}
          />
          {key.name.startsWith("C") && !key.name.startsWith("C#") && (
            <text className="key-label" x={(key.x + 0.5) * WHITE_WIDTH} y={WHITE_HEIGHT - 8}>
              {key.name}
            </text>
          )}
        </g>
      ))}
      {blacks.map((key) => (
        <rect
          key={key.midi}
          data-midi={key.midi}
          className={classesOf(key.midi, true)}
          x={key.x * WHITE_WIDTH}
          y={0}
          width={BLACK_KEY_WIDTH * WHITE_WIDTH}
          height={BLACK_HEIGHT}
          rx={2}
          {...handlers(key.midi)}
        />
      ))}
    </svg>
  );
}
