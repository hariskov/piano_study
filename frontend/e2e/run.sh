#!/bin/sh
# Runs e2e/smoke.ts inside the real app window with a fresh settings and library folder. Needs a display.
set -e
frontend="$(cd "$(dirname "$0")/.." && pwd)"
cd "$frontend"
npx vite build -c e2e/vite.config.ts --logLevel warn
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p test-results/screens
XDG_CONFIG_HOME="$work/config" XDG_DATA_HOME="$work/data" PP_SCREEN=devices \
    PP_E2E="$frontend/dist/e2e/smoke.js" PP_E2E_SCREENS="$frontend/test-results/screens" \
    ../bin/ui
