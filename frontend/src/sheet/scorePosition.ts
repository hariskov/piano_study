export interface StepCursor {
  timestamp(): number;
  atEnd(): boolean;
  next(): void;
  reset(): void;
}

const EPSILON = 1e-9;

function fraction(text: string): number {
  const [numerator, denominator = "1"] = text.split("/");
  return Number(numerator) / Number(denominator);
}

export function scorePosition(start: string, sectionStart: string | undefined, passLength: string | undefined): number {
  const position = fraction(start);
  if (sectionStart === undefined || passLength === undefined) {
    return position;
  }
  const length = fraction(passLength);
  return fraction(sectionStart) + (position - Math.floor(position / length + EPSILON) * length);
}

export function followTo(cursor: StepCursor, position: number): void {
  if (cursor.timestamp() > position + EPSILON) {
    cursor.reset();
  }
  while (!cursor.atEnd() && cursor.timestamp() < position - EPSILON) {
    cursor.next();
  }
}
