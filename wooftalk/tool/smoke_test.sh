#!/usr/bin/env bash
# Installs the APK on a running emulator, opens the app, and fails if it
# crashes. Leaves logcat + screenshots in ./smoke for debugging.
set -u
APK="$1"
PKG=com.wooftalk.dogtranslator
T="timeout 60"
mkdir -p smoke
shot() { echo "screenshot $1"; $T adb exec-out screencap -p > "smoke/$1.png" || echo "screencap failed"; }

echo "installing"
$T adb install -r -g "$APK" || exit 1
$T adb logcat -c
echo "launching"
$T adb shell am start -n "$PKG/com.wooftalk.wooftalk.MainActivity"
sleep 8; shot 1_launch
sleep 12; shot 2_home

size=$($T adb shell wm size | tr -d '\r' | awk '/Physical/{print $3}')
W=${size%x*}; H=${size#*x}
echo "screen ${W}x${H}"
if [ -n "$W" ] && [ -n "$H" ]; then
  echo "tap mic"
  $T adb shell input tap $((W / 2)) $((H * 68 / 100))
  sleep 3; shot 3_recording
  $T adb shell input tap $((W / 2)) $((H * 68 / 100))
  sleep 6; shot 4_after_stop
fi

echo "collecting logcat"
$T adb logcat -d > smoke/logcat.txt
pid=$($T adb shell pidof "$PKG" | tr -d '\r')
echo "---- crash lines ----"
grep -nE "FATAL EXCEPTION|Fatal signal|E AndroidRuntime|E flutter|Abort message" smoke/logcat.txt | head -60 || true
if [ -z "$pid" ] || grep -qE "FATAL EXCEPTION|Fatal signal" smoke/logcat.txt; then
  echo "App crashed or is not running"
  grep -nA40 -E "FATAL EXCEPTION|Fatal signal" smoke/logcat.txt | head -120
  exit 1
fi
echo "App is running (pid $pid)"
