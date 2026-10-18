#!/usr/bin/env bash
# Installs the APK on the running emulator, walks through onboarding and every
# tab, takes screenshots, and fails if the app crashes or logs an unhandled
# Dart exception.
#   usage: smoke_test.sh <apk> <output-dir>
set -uo pipefail

APK="$1"
OUT="$2"
PKG=com.goldenhour.golden_hour
mkdir -p "$OUT"

# Never let a single adb call hang the job.
ADB_BIN=$(command -v adb)
adb() { timeout 60 "$ADB_BIN" "$@"; }

fail() {
  echo "❌ $*"
  adb logcat -d > "$OUT/logcat.txt" || true
  echo "----- crash details -----"
  grep -A40 -E "FATAL EXCEPTION|Unhandled Exception|F/libc|Fatal signal" "$OUT/logcat.txt" | head -150 || true
  echo "----- flutter / app log -----"
  grep -E "flutter|golden_hour|AndroidRuntime|DEBUG|impeller|Impeller" "$OUT/logcat.txt" | tail -120 || true
  exit 1
}

shot() {
  sleep "${2:-4}"
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "📸 $1"
}

alive() {
  local pid
  pid=$(adb shell pidof "$PKG" | tr -d '\r')
  [ -n "$pid" ] || fail "App is not running after: $1"
  echo "✅ running after: $1 (pid $pid)"
}

# Screen geometry -> dp helpers
read -r W H < <(adb shell wm size | tail -1 | sed 's/.*: //; s/x/ /')
DENSITY=$(adb shell wm density | tail -1 | sed 's/.*: //' | tr -d '\r')
dp() { echo $(( $1 * DENSITY / 160 )); }
echo "Screen ${W}x${H} @ ${DENSITY}dpi"

echo "== Install"
adb install -r -g "$APK" || fail "install failed"

# Location: emulator GPS on Brighton Beach, location services on.
adb shell settings put secure location_mode 3 || true
adb emu geo fix -0.137 50.819 || true

adb logcat -c
echo "== Launch"
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
shot 01_onboarding_1 12
alive "launch"

# Onboarding: main button sits ~93dp above the bottom edge.
BTN_X=$((W / 2))
BTN_Y=$((H - $(dp 93)))
adb shell input tap "$BTN_X" "$BTN_Y"
shot 02_onboarding_2 3
adb shell input tap "$BTN_X" "$BTN_Y"
shot 03_onboarding_3 3
alive "onboarding pages"
adb shell input tap "$BTN_X" "$BTN_Y"   # "Use my location"
adb emu geo fix -0.137 50.819 || true
shot 04_today 15
alive "finish onboarding"

# Bottom navigation: 5 tabs across (screen width - 40dp), ~76dp above bottom.
NAV_Y=$((H - $(dp 76)))
tab() {
  local i=$1
  local inner=$((W - $(dp 40)))
  local x=$(( $(dp 20) + inner * (2 * i + 1) / 10 ))
  adb shell input tap "$x" "$NAV_Y"
}

tab 1; shot 05_ar_sun 8;   alive "AR tab"
tab 2; shot 06_compass 5;  alive "Compass tab"
tab 3; shot 07_planner 5;  alive "Planner tab"
adb shell input swipe $((W / 2)) $((H * 3 / 4)) $((W / 2)) $((H / 4)) 400
shot 08_planner_scrolled 3
tab 4; shot 09_moon 5;     alive "Moon tab"
tab 0; shot 10_today_again 5; alive "back to Today"

# Scroll the Today screen
adb shell input swipe $((W / 2)) $((H * 3 / 4)) $((W / 2)) $((H / 5)) 500
shot 11_today_scrolled 3
alive "scroll Today"

# Background / foreground cycle
adb shell input keyevent KEYCODE_HOME
sleep 3
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
shot 12_resume 6
alive "resume from background"

adb logcat -d > "$OUT/logcat.txt"
grep -E "E/flutter|FATAL EXCEPTION|AndroidRuntime" "$OUT/logcat.txt" > "$OUT/errors.txt" || true

if grep -q "FATAL EXCEPTION" "$OUT/logcat.txt"; then
  grep -A30 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -60
  fail "native crash found in logcat"
fi
if grep -q "Unhandled Exception" "$OUT/logcat.txt"; then
  grep -A15 "Unhandled Exception" "$OUT/logcat.txt" | head -60
  fail "unhandled Dart exception found in logcat"
fi

echo "Flutter error lines: $(wc -l < "$OUT/errors.txt")"
head -40 "$OUT/errors.txt" || true
echo "🎉 Smoke test passed"
