export interface InstrumentGroup {
  readonly name: string;
  readonly instruments: readonly (readonly [program: number, name: string])[];
}

// A selection of General MIDI programs; the numbers are what the output's synthesizer understands.
export const INSTRUMENT_GROUPS: readonly InstrumentGroup[] = [
  {
    name: "Pianos and keys",
    instruments: [[0, "Grand piano"], [1, "Bright piano"], [4, "Electric piano"], [6, "Harpsichord"], [8, "Celesta"], [10, "Music box"], [11, "Vibraphone"]],
  },
  { name: "Organs", instruments: [[16, "Drawbar organ"], [19, "Church organ"], [21, "Accordion"]] },
  {
    name: "Guitars",
    instruments: [[24, "Nylon guitar"], [25, "Steel guitar"], [26, "Jazz guitar"], [29, "Overdriven guitar"], [30, "Distortion guitar"]],
  },
  { name: "Strings and voices", instruments: [[40, "Violin"], [42, "Cello"], [46, "Harp"], [48, "String ensemble"], [52, "Choir"]] },
  {
    name: "Brass and winds",
    instruments: [[56, "Trumpet"], [57, "Trombone"], [60, "French horn"], [61, "Brass section"], [65, "Alto sax"], [71, "Clarinet"], [73, "Flute"]],
  },
  {
    name: "Space and synth",
    instruments: [[88, "New age pad"], [89, "Warm pad"], [91, "Choir pad"], [94, "Halo pad"], [95, "Sweep pad"], [96, "Rain"], [97, "Soundtrack"], [98, "Crystal"], [99, "Atmosphere"], [102, "Echoes"], [103, "Sci-fi"], [80, "Square lead"], [81, "Saw lead"]],
  },
];
