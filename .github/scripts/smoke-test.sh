#!/usr/bin/env bash
# End-to-end smoke test on an emulator: install, enable the keyboard, type with real taps
# on the soft keys and check the text that reaches the app.
set -euo pipefail

APK="$1"
PKG=com.bornomala.keyboard
IME="$PKG/.ime.BornomalaIME"
OUT=smoke
mkdir -p "$OUT"

adb install -r "$APK"
# The package manager may need a moment before the new input method is listed.
for i in $(seq 1 30); do
  adb shell ime list -a -s | grep -q "$PKG/" && break
  sleep 2
done
adb shell ime list -a -s
IME=$(adb shell ime list -a -s | tr -d '\r' | grep "$PKG/" | head -1)
adb shell ime enable "$IME"
adb shell ime set "$IME"
adb logcat -c

adb shell am start -W -n "$PKG/.ui.MainActivity"
sleep 4

dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb shell cat /sdcard/ui.xml; }

# Tap the "Try typing here" field.
BOUNDS=$(dump | grep -o 'class="android.widget.EditText"[^>]*bounds="[^"]*"' | head -1 | grep -o 'bounds="[^"]*"' | grep -oE '[0-9]+' | tr '\n' ' ')
read -r X1 Y1 X2 Y2 <<<"$BOUNDS"
adb shell input tap $(( (X1 + X2) / 2 )) $(( (Y1 + Y2) / 2 ))
sleep 3

# Find the keyboard window and compute key positions (strip 46dp, rows 54dp, 10 units wide).
DENSITY=$(adb shell wm density | grep -oE '[0-9]+' | tail -1)
WIDTH=$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f1)
IME_TOP=$(adb shell dumpsys window windows | grep -A40 'Window{.*InputMethod' | grep -m1 -oE 'frame=\[[0-9]+,[0-9]+\]' | grep -oE '[0-9]+' | tail -1)
echo "density=$DENSITY width=$WIDTH imeTop=$IME_TOP"
dp() { echo $(( $1 * DENSITY / 160 )); }
key() { # $1 = row (0-3), $2 = x center in tenths of the keyboard width units*10
  local y=$(( IME_TOP + $(dp 46) + $1 * $(dp 54) + $(dp 27) ))
  local x=$(( WIDTH * $2 / 100 ))
  adb shell input tap "$x" "$y"
  sleep 0.4
}
# Bangla phonetic is the default language: a m i <space>
key 1 10   # a
key 2 80   # m
key 0 75   # i
key 3 55   # space
sleep 1
TEXT=$(dump | grep -o 'class="android.widget.EditText"[^>]*' | head -1)
echo "$TEXT" | grep -o 'text="[^"]*"' || true
adb exec-out screencap -p > "$OUT/typed.png" || true

# Switch to English with the globe key and type "hi".
key 3 20   # globe
key 1 60   # h
key 0 75   # i
sleep 1
TEXT2=$(dump | grep -o 'class="android.widget.EditText"[^>]*' | head -1)
echo "$TEXT2" | grep -o 'text="[^"]*"' || true

adb logcat -d > "$OUT/logcat.txt"
if grep -E "FATAL EXCEPTION|AndroidRuntime: Process: $PKG" "$OUT/logcat.txt"; then
  grep -A30 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -60
  echo "::error::The app crashed during the smoke test"
  exit 1
fi
echo "$TEXT" | grep -q 'আমি' || { echo "::error::Phonetic typing did not produce আমি"; exit 1; }
echo "$TEXT2" | grep -qi 'আমি hi' || { echo "::error::English typing after language switch failed"; exit 1; }
echo "Smoke test passed"
