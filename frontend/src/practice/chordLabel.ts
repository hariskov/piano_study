import type { TargetEvent } from "../api/protocol";

export type ChordLabels = "name" | "notes" | "both";

export function targetLabel(target: TargetEvent, labels: ChordLabels): string {
  const notes = target.notes.map((note) => note.pitch).join(" ");
  if (target.name === undefined || labels === "notes") {
    return notes;
  }
  return labels === "name" ? target.name : `${target.name} · ${notes}`;
}
