#!/usr/bin/env bash
# Builds the AppImage from what Maven built: the UI (frontend/dist), Electron's own files and the
# jlink runtime (backend/pp-app/target/runtime). Run through ./mvnw -Pappimage package.
set -euo pipefail

root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
frontend="$root/frontend"
runtime="$root/backend/pp-app/target/runtime"
out="$frontend/target/appimage"
appdir="$out/AppDir"
tool_version="1.9.1"
tool="${APPIMAGETOOL:-$HOME/.cache/piano-player/appimagetool-$tool_version-x86_64.AppImage}"
version="$(sed -n 's/^  "version": "\(.*\)",$/\1/p' "$frontend/package.json")"

fail() {
    echo "build-appimage: $*" >&2
    exit 1
}

[[ -f "$frontend/dist/electron/main.cjs" ]] || fail "the UI isn't built; run ./mvnw package first"
[[ -x "$runtime/bin/pp" ]] || fail "the Java runtime isn't linked; run ./mvnw -Pappimage package"
if [[ ! -x "$tool" ]]; then
    fail "appimagetool $tool_version is needed. Download it from
  https://github.com/AppImage/appimagetool/releases/download/$tool_version/appimagetool-x86_64.AppImage
save it as $tool and make it executable (or point APPIMAGETOOL at it)."
fi

electron="$frontend/node_modules/electron/dist"
if [[ ! -x "$electron/electron" ]]; then
    (cd "$frontend" && node node_modules/electron/install.js)
fi

rm -rf "$appdir"
lib="$appdir/usr/lib/piano-player"
mkdir -p "$lib"
cp -a "$electron/." "$lib/"
mv "$lib/electron" "$lib/piano-player"
rm -f "$lib/resources/default_app.asar"
mkdir -p "$lib/resources/app"
cp -a "$frontend/dist" "$lib/resources/app/"
cp "$frontend/package.json" "$lib/resources/app/"
cp -a "$runtime" "$lib/resources/backend"

cat > "$appdir/AppRun" <<'RUN'
#!/bin/sh
here="$(dirname "$(readlink -f "$0")")"
exec "$here/usr/lib/piano-player/piano-player" "$@"
RUN
chmod +x "$appdir/AppRun"

cat > "$appdir/piano-player.desktop" <<'DESKTOP'
[Desktop Entry]
Type=Application
Name=Piano Player
Comment=Practise the piano with a MIDI keyboard
Exec=AppRun
Icon=piano-player
Categories=Education;Music;
Keywords=piano;midi;music;practice;
Terminal=false
DESKTOP
cp "$root/packaging/icon.png" "$appdir/piano-player.png"
ln -s piano-player.png "$appdir/.DirIcon"

image="$out/Piano-Player-$version-x86_64.AppImage"
ARCH=x86_64 "$tool" --appimage-extract-and-run --no-appstream "$appdir" "$image"
rm -rf "$appdir"
echo "Built $image"
