#!/usr/bin/env bash
# Builds one self-extracting shell executable from what Maven built: the UI (frontend/dist),
# Electron's own files and the jlink runtime (backend/pp-app/target/runtime).
# Run through ./mvnw -Pbundle package.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
frontend="$root/frontend"
runtime="$root/backend/pp-app/target/runtime"
out="$frontend/target/bundle"
app="$out/piano-player"
version="$(sed -n 's/^  "version": "\(.*\)",$/\1/p' "$frontend/package.json")"

fail() {
    echo "build-bundle: $*" >&2
    exit 1
}

[[ -f "$frontend/dist/electron/main.cjs" ]] || fail "the UI isn't built; run ./mvnw package first"
[[ -x "$runtime/bin/pp" ]] || fail "the Java runtime isn't linked; run ./mvnw -Pbundle package"

electron="$frontend/node_modules/electron/dist"
if [[ ! -x "$electron/electron" ]]; then
    (cd "$frontend" && node node_modules/electron/install.js)
fi

rm -rf "$out"
mkdir -p "$app"
cp -a "$electron/." "$app/"
mv "$app/electron" "$app/piano-player"
rm -f "$app/resources/default_app.asar"
mkdir -p "$app/resources/app"
cp -a "$frontend/dist" "$app/resources/app/"
cp "$frontend/package.json" "$app/resources/app/"
cp -a "$runtime" "$app/resources/backend"

payload="$out/payload.tar.xz"
tar -C "$out" -c piano-player | xz -T0 -6 > "$payload"
id="$version-$(sha256sum "$payload" | cut -c1-12)"

# The payload is unpacked once per build into the user's cache; older unpacked builds are removed.
header="$(cat <<'RUN'
#!/bin/sh
set -eu
cache="${XDG_CACHE_HOME:-$HOME/.cache}/piano-player/app"
dir="$cache/@ID@"
if [ ! -x "$dir/piano-player" ]; then
    mkdir -p "$cache"
    tmp="$(mktemp -d "$cache/.unpack.XXXXXX")"
    tail -n +@SKIP@ "$(readlink -f "$0")" | xz -d | tar -x -C "$tmp"
    find "$cache" -mindepth 1 -maxdepth 1 ! -path "$tmp" -exec rm -rf {} +
    mv "$tmp/piano-player" "$dir"
    rmdir "$tmp"
fi
exec "$dir/piano-player" "$@"
RUN
)"
skip=$(($(printf '%s\n' "$header" | wc -l) + 1))
bundle="$out/piano-player-$version-x86_64.sh"
{
    printf '%s\n' "$header" | sed -e "s/@ID@/$id/" -e "s/@SKIP@/$skip/"
    cat "$payload"
} > "$bundle"
chmod +x "$bundle"
rm -rf "$app" "$payload"
echo "Built $bundle"
