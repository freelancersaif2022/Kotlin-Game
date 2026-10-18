#!/usr/bin/env bash
# Installs the APK on a running emulator, opens the app, and fails if it
# crashes. Leaves logcat + screenshots in ./smoke for debugging.
set -u
APK="$1"
PKG=com.wooftalk.dogtranslator
T="timeout 60"
mkdir -p smoke
shot() {
  echo "screenshot $1"
  if $T adb exec-out screencap -p > "smoke/$1.png"; then
    echo "B64PNG $1 $(base64 -w0 "smoke/$1.png")"
  else
    echo "screencap failed"
  fi
}
alive() { $T adb shell pidof "$PKG" | tr -d '\r'; }

echo "installing"
$T adb install -r -g "$APK" || exit 1
$T adb logcat -c
# Stream logcat to a file so it survives even if the device goes away.
adb logcat -v time > smoke/logcat.txt 2>&1 &
echo "launching"
$T adb shell am start -W -n "$PKG/com.wooftalk.wooftalk.MainActivity"
for i in 1 2 3 4 5 6; do
  sleep 5
  echo "t=$((i * 5))s pid=$(alive)"
done
shot 1_home
echo "top activity: $($T adb shell dumpsys activity activities | grep -m1 topResumedActivity)"

size=$($T adb shell wm size | tr -d '\r' | awk '/Physical/{print $3}')
W=${size%x*}; H=${size#*x}
echo "screen ${W}x${H}"
if [ -n "$W" ] && [ -n "$H" ] && [ -n "$(alive)" ]; then
  echo "tap mic"
  $T adb shell input tap $((W / 2)) $((H * 68 / 100))
  sleep 4; shot 2_recording
  $T adb shell input tap $((W / 2)) $((H * 68 / 100))
  sleep 8; shot 3_after_stop
fi

pid=$(alive)
sleep 1
kill %1 2>/dev/null
echo "---- app log lines ----"
grep -E "flutter|$PKG|AndroidRuntime|DEBUG" smoke/logcat.txt | grep -v "^--" | tail -80
echo "---- crash lines ----"
grep -nE "FATAL EXCEPTION|Fatal signal|E/AndroidRuntime|Abort message" smoke/logcat.txt | head -40 || true
if grep -qE "FATAL EXCEPTION|Fatal signal" smoke/logcat.txt; then
  echo "App crashed"
  grep -nA40 -E "FATAL EXCEPTION|Fatal signal" smoke/logcat.txt | head -120
  exit 1
fi
if [ -z "$pid" ]; then
  echo "App is not running (no crash logged - emulator may have died)"
  exit 1
fi
echo "App is running (pid $pid)"
