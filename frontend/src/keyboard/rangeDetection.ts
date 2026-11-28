import { pitchName } from "../music/pitch";

export type Detection =
  | { step: "lowest" }
  | { step: "highest"; first: number }
  | { step: "done"; range: string };

export const START_DETECTION: Detection = { step: "lowest" };

export function detectStep(detection: Detection, midi: number): Detection {
  switch (detection.step) {
    case "lowest":
      return { step: "highest", first: midi };
    case "highest":
      if (midi === detection.first) {
        return detection;
      }
      return {
        step: "done",
        range: `${pitchName(Math.min(midi, detection.first))}-${pitchName(Math.max(midi, detection.first))}`,
      };
    case "done":
      return detection;
  }
}
