// Generated from the Java API records (pp-api). Do not edit by hand: change the records, then run
// ./mvnw -pl backend/pp-api -am test -Dprotocol.write=true to write this file again.

export interface AdjustParams {
  hands?: string;
  level?: number;
  accidentals?: boolean;
  mixed?: boolean;
  chordTypes?: string[];
  inversions?: number[];
}

export interface BeatEvent {
  accent: boolean;
}

export interface DeviceInfo {
  id: string;
  name: string;
  description: string;
}

export interface DeviceList {
  inputs: DeviceInfo[];
  outputs: DeviceInfo[];
}

export interface EvalEvent {
  result: string;
  pitch?: string;
  grade?: string;
  offsetMs?: number;
}

export interface History {
  sessions: PastSession[];
}

export interface HistoryParams {
  songId?: string;
  exercise?: string;
}

export interface ImportParams {
  path?: string;
  grid?: number;
  triplets?: boolean;
  splitPoint?: string;
  exclude?: string[];
}

export interface ImportPart {
  id: string;
  name: string;
  instrument: string;
  staffCount: number;
  noteCount: number;
  lowest?: string;
  highest?: string;
}

export interface InputDeviceEvent {
  input: string;
}

export interface InputMessage {
  type: string;
  pitch?: string;
  midi?: number;
  velocity?: number;
  down?: boolean;
  t: number;
}

export interface KeyParams {
  pitch?: string;
  velocity?: number;
}

export interface MidiPreview {
  title: string;
  measureCount: number;
  timeSignature: string;
  key: string;
  tempo: number;
  parts: ImportPart[];
  warnings: string[];
}

export interface MonitorParams {
  input?: string;
}

export interface MusicXml {
  xml: string;
}

export interface NoteInfo {
  pitch: string;
  midi: number;
  hand: string;
}

export interface PartSummary {
  id: string;
  name: string;
  staffCount: number;
  noteCount: number;
  lowest?: string;
  highest?: string;
}

export interface PastSession {
  finishedAt: string;
  exercise: string;
  songId?: string;
  part?: string;
  hands: string;
  mode: string;
  tempo: number;
  tempoPercent?: number;
  fromBar?: number;
  toBar?: number;
  accuracyPercent: number;
  timed: boolean;
  completedTargets: number;
  wrongKeys: number;
  perfect: number;
  good: number;
  ok: number;
  missed: number;
  elapsedMs: number;
  passes: number[];
  mistakesByBar: Record<string, number>;
}

export interface SessionStartParams {
  input?: string;
  exercise?: string;
  mode?: string;
  tempo?: number;
  key?: string;
  hands?: string;
  range?: string;
  keyboard?: string;
  noteValue?: string;
  time?: string;
  notes?: number;
  countIn?: number;
  seed?: number;
  metronome?: string;
  level?: number;
  accidentals?: boolean;
  mixed?: boolean;
  chordTypes?: string[];
  inversions?: number[];
  songId?: string;
  part?: string;
  outOfRange?: string;
  tempoPercent?: number;
  playback?: string;
  fromBar?: number;
  toBar?: number;
  repeats?: number;
}

export interface SessionStarted {
  seed: number;
  tempo: number;
  songTempo?: number;
  notices: string[];
  sectionStart?: string;
  passLength?: string;
}

export interface SettingsUpdate {
  input?: string;
  output?: string;
  keyboards?: Record<string, string>;
  tempos?: Record<string, number>;
  latencyOffsetMs?: number;
}

export interface SettingsView {
  input?: string;
  output?: string;
  keyboards: Record<string, string>;
  tempos: Record<string, number>;
  latencyOffsetMs: number;
}

export interface SongDetails {
  id: string;
  title: string;
  composer?: string;
  file: string;
  midiFile?: string;
  measureCount: number;
  timeSignature: string;
  key: string;
  tempo: number;
  parts: PartSummary[];
  warnings: string[];
}

export interface SongId {
  id?: string;
}

export interface SongList {
  songs: SongSummary[];
}

export interface SongSummary {
  id: string;
  title: string;
  fileName: string;
  midiFile?: string;
  partCount: number;
  measureCount: number;
  sourceFile: string;
  format: string;
  importedAt: string;
  warnings: string[];
}

export interface StateEvent {
  state: string;
}

export interface SummaryEvent {
  timed: boolean;
  completedTargets: number;
  correctNotes: number;
  wrongKeys: number;
  perfect: number;
  good: number;
  ok: number;
  missed: number;
  accuracyPercent: number;
  elapsedMs: number;
  passes: number[];
}

export interface TargetEvent {
  start: string;
  name?: string;
  notes: NoteInfo[];
}

export interface TempoParams {
  bpm?: number;
}

export interface ApiMethods {
  "devices.list": { params: Record<string, never>; result: DeviceList };
  "devices.monitor": { params: MonitorParams; result: null };
  "devices.rescan": { params: Record<string, never>; result: DeviceList };
  "devices.stopMonitor": { params: Record<string, never>; result: null };
  "keyboard.press": { params: KeyParams; result: null };
  "keyboard.release": { params: KeyParams; result: null };
  "library.delete": { params: SongId; result: null };
  "library.import": { params: ImportParams; result: SongSummary };
  "library.list": { params: Record<string, never>; result: SongList };
  "library.musicXml": { params: SongId; result: MusicXml };
  "library.open": { params: SongId; result: SongDetails };
  "library.preview": { params: ImportParams; result: MidiPreview };
  "progress.history": { params: HistoryParams; result: History };
  "session.adjust": { params: AdjustParams; result: null };
  "session.changeTempo": { params: TempoParams; result: null };
  "session.pause": { params: Record<string, never>; result: null };
  "session.resume": { params: Record<string, never>; result: null };
  "session.start": { params: SessionStartParams; result: SessionStarted };
  "session.stop": { params: Record<string, never>; result: null };
  "settings.get": { params: Record<string, never>; result: SettingsView };
  "settings.update": { params: SettingsUpdate; result: SettingsView };
}

export interface ApiEvents {
  "beat": BeatEvent;
  "devices.changed": DeviceList;
  "eval": EvalEvent;
  "input": InputMessage;
  "session.inputLost": InputDeviceEvent;
  "session.inputRestored": InputDeviceEvent;
  "session.state": StateEvent;
  "session.summary": SummaryEvent;
  "session.target": TargetEvent;
}
