# Piano Player

Piano learning desktop app: Java 25 backend (Maven multi-module) and an Electron + React + TypeScript UI. Linux only.

Docs: `docs/01-vision.md`, `docs/02-specification.md`, `docs/03-architecture-plan.md`, `docs/04-phases.md`.

## Rules

- Clean architecture and SOLID, as described in `docs/03-architecture-plan.md` §2A. The core modules (`pp-model`, `pp-engine`, `pp-generator`) never depend on I/O, MIDI or web libraries.
- No comments or Javadoc that just restate names or signatures. A comment is only for a non-obvious *why*.
- Git commits and PRs must not mention Claude or AI: no `Co-Authored-By` lines and no "Generated with" footers.
- Keep the specification in sync when a decision changes.

## Workflow

- All work goes through pull requests, which the user reviews on GitHub. Never push directly to `main`.
- Keep each PR small: under ~500 changed lines or 20 files. Split larger work into a series of focused PRs.
- One branch per PR, named `<type>/<short-topic>` (for example `feat/midi-device-listing`, `docs/specification`).
