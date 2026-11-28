import { Icon } from "../icons/Icon";
import type { PastSession, SummaryEvent } from "../api/protocol";
import { previousOf, troubleBars } from "../progress/history";

interface Props {
  summary: SummaryEvent;
  history?: readonly PastSession[];
  onAgain: () => void;
  onChangeSettings: () => void;
}

export function formatTime(milliseconds: number): string {
  const seconds = Math.round(milliseconds / 1000);
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
}

export function ResultsView({ summary, history = [], onAgain, onChangeSettings }: Props) {
  const previous = previousOf(history);
  const latest = history.at(-1);
  const bars = latest ? troubleBars(latest) : [];
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
      <h2 className="with-icon">
        <Icon name="star" />
        Well played
      </h2>
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
      {(previous || bars.length > 0) && (
        <div className="card progress-note">
          {previous && (
            <p>
              Last time {previous.accuracyPercent}% · now {summary.accuracyPercent}%
            </p>
          )}
          {bars.length > 0 && <p>Most mistakes in {bars.length === 1 ? "bar" : "bars"} {bars.join(", ")}</p>}
        </div>
      )}
      {summary.passes.length > 1 && (
        <ol className="card passes" aria-label="Passes">
          {summary.passes.map((accuracy, index) => (
            <li key={index}>
              <span>Pass {index + 1}</span>
              <span className="pass-bar" style={{ width: `${accuracy}%` }} />
              <strong>{accuracy}%</strong>
            </li>
          ))}
        </ol>
      )}
      <div className="actions">
        <button type="button" onClick={onChangeSettings}>
          <Icon name="settings" />
          Change settings
        </button>
        <button type="button" className="primary big" onClick={onAgain}>
          <Icon name="again" />
          Practise again
        </button>
      </div>
    </section>
  );
}
