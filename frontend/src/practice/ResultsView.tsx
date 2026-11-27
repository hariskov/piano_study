import type { SummaryEvent } from "../api/protocol";

interface Props {
  summary: SummaryEvent;
  onAgain: () => void;
  onChangeSettings: () => void;
}

export function formatTime(milliseconds: number): string {
  const seconds = Math.round(milliseconds / 1000);
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
}

export function ResultsView({ summary, onAgain, onChangeSettings }: Props) {
  const stats: [string, string | number][] = summary.timed
    ? [
        ["Perfect", summary.perfect],
        ["Good", summary.good],
        ["OK", summary.ok],
        ["Missed", summary.missed],
        ["Wrong keys", summary.wrongKeys],
        ["Time", formatTime(summary.elapsedMs)],
      ]
    : [
        ["Notes and chords", summary.completedTargets],
        ["Wrong keys", summary.wrongKeys],
        ["Time", formatTime(summary.elapsedMs)],
      ];
  return (
    <section className="screen results">
      <h2>Well played</h2>
      <div className="card results-card">
        <div className="accuracy">
          <span aria-label="Accuracy">{summary.accuracyPercent}%</span>
          <small>accuracy</small>
        </div>
        <dl className="stats">
          {stats.map(([label, value]) => (
            <div key={label}>
              <dt>{label}</dt>
              <dd>{value}</dd>
            </div>
          ))}
        </dl>
      </div>
      <div className="actions">
        <button type="button" onClick={onChangeSettings}>
          Change settings
        </button>
        <button type="button" className="primary big" onClick={onAgain}>
          Practise again
        </button>
      </div>
    </section>
  );
}
