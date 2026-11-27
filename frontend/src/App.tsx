import { markingFor } from "./tempo/tempoMarking";

export function App() {
  return (
    <main>
      <h1>Piano Player</h1>
      <p>Andante is {markingFor(90).name}.</p>
    </main>
  );
}
