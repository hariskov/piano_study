# Piano Player

Piano learning desktop app: Java 25 backend (Maven multi-module) and a React + TypeScript UI in a WebKitGTK window (`frontend/shell/piano-player.js`, run with GJS). Linux only.

Docs: `docs/01-vision.md`, `docs/02-specification.md`, `docs/03-architecture-plan.md`, `docs/04-phases.md`, `docs/05-api.md` (backend API reference).

## Build

- To run the app: GJS and WebKitGTK 6.0 (openSUSE: `sudo zypper in gjs typelib-1_0-WebKit-6_0`). `bin/ui` starts it from the checkout.
- `./mvnw verify` builds and runs all tests, backend and frontend (JDK 25, Maven 3.10 via the wrapper). The frontend build uses the system Node.js (24 or newer; openSUSE: `sudo zypper in nodejs26 npm26`) and runs `npm ci`, `npm run build` (type check plus Vite) and `npm test` (Vitest with happy-dom).
- After changing an API record, method or event: `./mvnw -pl backend/pp-api -am test -Dprotocol.write=true` regenerates `frontend/src/api/protocol.ts` (the build fails while it is out of date).
- Frontend only: `cd frontend && npm test` (or `npm run dev`). `npm run e2e` runs the smoke test in the real window (needs a display; after `./mvnw -q package -DskipTests`).
- `./mvnw -q package -DskipTests && bin/pp <command>` runs the CLI (`pp-app`) against the real MIDI system.
- `./mvnw -Pbundle package -DskipTests` also builds the self-extracting executable `frontend/target/bundle/piano-player-<version>-x86_64.sh` (`packaging/build-bundle.sh`). It needs the JDK's jmods (openSUSE: `java-25-openjdk-jmods`).
- Keep dependencies few: check what a new package pulls in (`npm ls --all`, the lock file's size) before adding it, and prefer a short script to a large tool.

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
