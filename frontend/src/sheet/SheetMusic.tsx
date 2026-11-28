import { useEffect, useRef, useState } from "react";
import type { OpenSheetMusicDisplay } from "opensheetmusicdisplay";
import { followTo, type StepCursor } from "./scorePosition";

interface Props {
  xml: string;
  position?: number;
}

function stepCursor(osmd: OpenSheetMusicDisplay): StepCursor {
  const cursor = osmd.cursor;
  return {
    timestamp: () => cursor.Iterator.currentTimeStamp.RealValue,
    atEnd: () => cursor.Iterator.EndReached,
    next: () => cursor.next(),
    reset: () => cursor.reset(),
  };
}

export function SheetMusic({ xml, position }: Props) {
  const container = useRef<HTMLDivElement>(null);
  const [display, setDisplay] = useState<OpenSheetMusicDisplay>();
  const [osmd, setOsmd] = useState<OpenSheetMusicDisplay>();
  const [problem, setProblem] = useState<string>();

  useEffect(() => {
    let current = true;
    void (async () => {
      try {
        const { OpenSheetMusicDisplay } = await import("opensheetmusicdisplay");
        if (current && container.current) {
          setDisplay(
            new OpenSheetMusicDisplay(container.current, {
              backend: "svg",
              autoResize: true,
              drawTitle: false,
              drawPartNames: true,
              followCursor: true,
            }),
          );
        }
      } catch (error) {
        if (current) {
          setProblem(`The sheet music couldn't be drawn: ${(error as Error).message}`);
        }
      }
    })();
    return () => {
      current = false;
    };
  }, []);

  // The same display is reused when the score changes, as it does after every note on the exercise staff.
  useEffect(() => {
    if (!display) {
      return;
    }
    let current = true;
    setOsmd(undefined);
    setProblem(undefined);
    display.load(xml).then(
      () => {
        if (current) {
          display.render();
          setOsmd(display);
        }
      },
      (error: Error) => {
        if (current) {
          setProblem(`The sheet music couldn't be drawn: ${error.message}`);
        }
      },
    );
    return () => {
      current = false;
    };
  }, [display, xml]);

  useEffect(() => {
    if (!osmd) {
      return;
    }
    if (position === undefined) {
      osmd.cursor.hide();
      return;
    }
    osmd.cursor.show();
    followTo(stepCursor(osmd), position);
  }, [osmd, position]);

  return (
    <div className="card sheet-music" aria-label="Sheet music">
      {problem && <p className="problem">{problem}</p>}
      <div ref={container} />
    </div>
  );
}
