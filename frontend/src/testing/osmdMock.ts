import { vi } from "vitest";

const STOPS = [0, 0.25, 0.5, 0.75, 1, 1.25];

export class FakeOsmd {
  loaded?: string;
  rendered = false;
  readonly cursor = {
    shown: false,
    index: 0,
    get position() {
      return STOPS[this.index];
    },
    show() {
      this.shown = true;
    },
    hide() {
      this.shown = false;
    },
    next() {
      this.index = Math.min(this.index + 1, STOPS.length - 1);
    },
    reset() {
      this.index = 0;
    },
    get Iterator() {
      const index = this.index;
      return { currentTimeStamp: { RealValue: STOPS[index] }, EndReached: index >= STOPS.length - 1 };
    },
  };

  constructor() {
    osmdInstances.push(this);
  }

  async load(xml: string) {
    if ((globalThis as { __osmdFailNextLoad?: boolean }).__osmdFailNextLoad) {
      throw new Error("not MusicXML");
    }
    this.loaded = xml;
  }

  render() {
    this.rendered = true;
  }

  clear() {}
}

export const osmdInstances: FakeOsmd[] = [];

vi.mock("opensheetmusicdisplay", () => ({ OpenSheetMusicDisplay: FakeOsmd }));
