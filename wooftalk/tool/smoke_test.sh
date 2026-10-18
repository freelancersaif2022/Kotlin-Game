#!/usr/bin/env bash
# Installs the APK on a running emulator, opens the app, and fails if it
# crashes. Leaves logcat + screenshots in ./smoke for debugging.
set -u
APK="$1"
PKG=com.wooftalk.dogtranslator
mkdir -p smoke
adb install -r -g "$APK" || exit 1
adb logcat -c
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
sleep 8
adb exec-out screencap -p > smoke/1_launch.png
sleep 12
adb exec-out screencap -p > smoke/2_home.png
# Tap the mic button (bottom-centre area), talk-less recording for 3s, stop.
read -r W H < <(adb shell wm size | awk -F'[ x]' '/Physical/{print $3, $4}')
adb shell input tap $((W / 2)) $((H * 68 / 100))
sleep 3
adb exec-out screencap -p > smoke/3_recording.png
adb shell input tap $((W / 2)) $((H * 68 / 100))
sleep 6
adb exec-out screencap -p > smoke/4_after_stop.png
adb logcat -d > smoke/logcat.txt
pid=$(adb shell pidof "$PKG" | tr -d '\r')
echo "---- crash lines ----"
grep -nE "FATAL EXCEPTION|Fatal signal|E AndroidRuntime|E flutter|Abort message" smoke/logcat.txt | head -60 || true
if [ -z "$pid" ] || grep -qE "FATAL EXCEPTION|Fatal signal" smoke/logcat.txt; then
  echo "App crashed or is not running"
  grep -nA40 -E "FATAL EXCEPTION|Fatal signal" smoke/logcat.txt | head -120
  exit 1
fi
echo "App is running (pid $pid)"
