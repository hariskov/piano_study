# Piano Player

Piano learning desktop app: Java 25 backend (Maven multi-module) and an Electron + React + TypeScript UI. Linux only.

Docs: `docs/01-vision.md`, `docs/02-specification.md`, `docs/03-architecture-plan.md`, `docs/04-phases.md`, `docs/05-api.md` (backend API reference).

## Build

- `./mvnw verify` builds and runs all tests, backend and frontend (JDK 25, Maven 3.10 via the wrapper). The frontend build downloads its own Node into `frontend/target/node` and runs `npm ci`, `npm run build` (type check plus Vite) and `npm test` (Vitest). No system Node is needed.
- After changing an API record, method or event: `./mvnw -pl backend/pp-api -am test -Dprotocol.write=true` regenerates `frontend/src/api/protocol.ts` (the build fails while it is out of date).
- Frontend only: `cd frontend && PATH=$PWD/target/node:$PATH npm test` (or `npm run dev`) once Maven has installed Node.
- `./mvnw -q package -DskipTests && bin/pp <command>` runs the CLI (`pp-app`) against the real MIDI system.

## Rules

- Group ID and base package: `bg.petarh.piano`. New backend modules go under `backend/` with the `backend` POM as their parent.
- Test first: write the failing unit test, then the code (JUnit in the backend, Vitest in the frontend). All hardware (MIDI in/out) sits behind interfaces so everything can be tested without a device.
- Clean architecture and SOLID, as described in `docs/03-architecture-plan.md` §2A. The core modules (`pp-model`, `pp-engine`, `pp-generator`) never depend on I/O, MIDI or web libraries.
- No comments or Javadoc that just restate names or signatures. A comment is only for a non-obvious *why*.
- Git commits and PRs must not mention Claude or AI: no `Co-Authored-By` lines and no "Generated with" footers.
- **Keep the documentation up to date in the same PR as the change:** the specification (requirements), the architecture (structure, modules, design decisions), the status table in `docs/04-phases.md`, `README.md` and this file. A PR that changes behaviour or structure without updating the docs it affects is not finished.

## Workflow

- All work goes through pull requests, which the user reviews on GitHub. Never push directly to `main`.
- Keep each PR small: under ~500 changed lines or 20 files. Split larger work into a series of focused PRs.
- One branch per PR, named `<type>/<short-topic>` (for example `feat/midi-device-listing`, `docs/specification`).
