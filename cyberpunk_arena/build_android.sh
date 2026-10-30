#!/usr/bin/env bash
# Builds the Android debug APK from the command line.
#
# Requirements:
#   * Godot 4.4.1 editor binary            (GODOT, default: godot on PATH)
#   * Godot 4.4.1 export templates          (~/.local/share/godot/export_templates/4.4.1.stable/)
#   * Android SDK with platform-tools + build-tools (ANDROID_SDK, default: $ANDROID_HOME)
#   * JDK 17+                               (JAVA_HOME)
# Usage: ./build_android.sh [output.apk]
set -euo pipefail
cd "$(dirname "$0")"

GODOT="${GODOT:-godot}"
ANDROID_SDK="${ANDROID_SDK:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}"
OUT="${1:-../build/CyberpunkArena-debug.apk}"
KEYSTORE="${DEBUG_KEYSTORE:-$HOME/.android/debug.keystore}"
VERSION="$("$GODOT" --headless --version | cut -d. -f1-2)"
SETTINGS="${XDG_CONFIG_HOME:-$HOME/.config}/godot/editor_settings-$VERSION.tres"

# 1. Debug keystore (same values Android Studio uses).
if [ ! -f "$KEYSTORE" ]; then
  mkdir -p "$(dirname "$KEYSTORE")"
  keytool -genkeypair -v -keystore "$KEYSTORE" -storepass android -alias androiddebugkey \
    -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi

# 2. Import project once so the editor settings file exists, then point it at the SDK / JDK / keystore.
"$GODOT" --headless --path . --import >/dev/null 2>&1 || true
mkdir -p "$(dirname "$SETTINGS")"
[ -f "$SETTINGS" ] || printf '[gd_resource type="EditorSettings" format=3]\n\n[resource]\n' > "$SETTINGS"
python3 - "$SETTINGS" "$ANDROID_SDK" "$JAVA_HOME" "$KEYSTORE" <<'PY'
import sys
path, sdk, jdk, ks = sys.argv[1:]
values = {
    "export/android/android_sdk_path": sdk,
    "export/android/java_sdk_path": jdk,
    "export/android/debug_keystore": ks,
    "export/android/debug_keystore_user": "androiddebugkey",
    "export/android/debug_keystore_pass": "android",
}
lines = [l for l in open(path).read().split("\n") if not any(l.startswith(k + " ") for k in values)]
i = lines.index("[resource]")
for k, v in values.items():
    lines.insert(i + 1, f'{k} = "{v}"')
open(path, "w").write("\n".join(lines))
PY

# 3. Export.
mkdir -p "$(dirname "$OUT")"
"$GODOT" --headless --path . --export-debug "Android" "$OUT"
echo "APK written to $OUT"
