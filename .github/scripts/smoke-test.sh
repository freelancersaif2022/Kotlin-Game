#!/usr/bin/env bash
# End-to-end smoke test on an emulator: install, enable the keyboard, type with real taps on
# the soft keys and check the text that reaches the app. Key positions come from the keyboard's
# own geometry log (enabled through the BornomalaKeys log tag).
set -uo pipefail

APK="$1"
PKG=com.bornomala.keyboard
OUT=smoke
mkdir -p "$OUT"
fail() { echo "::error::$1"; adb logcat -d > "$OUT/logcat.txt"; grep -A30 "FATAL EXCEPTION" "$OUT/logcat.txt" | head -60; exit 1; }

adb install -r "$APK" || fail "install failed"
IME=""
for i in $(seq 1 30); do
  IME=$(adb shell ime list -a -s | tr -d '\r' | grep "^$PKG/" | head -1)
  [ -n "$IME" ] && break
  sleep 2
done
[ -n "$IME" ] || fail "input method not registered"
adb shell ime enable "$IME"
adb shell ime set "$IME"
adb shell setprop log.tag.BornomalaKeys DEBUG
adb logcat -c

adb shell am start -W -n "$PKG/.ui.MainActivity"
sleep 4

dump() { adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb shell cat /sdcard/ui.xml; }
field_text() { dump | grep -o '<node [^>]*class="android.widget.EditText"[^>]*>' | head -1 | grep -o ' text="[^"]*"'; }

# Focus the "Try typing here" field.
BOUNDS=""
for i in $(seq 1 15); do  # the first frame can take a while on a cold emulator
  BOUNDS=$(dump | grep -o '<node [^>]*class="android.widget.EditText"[^>]*>' | head -1 | grep -o 'bounds="[^"]*"' | grep -oE '[0-9]+' | tr '\n' ' ')
  [ -n "$BOUNDS" ] && break
  sleep 2
done
[ -n "$BOUNDS" ] || fail "try-typing field not found"
read -r X1 Y1 X2 Y2 <<<"$BOUNDS"
adb shell input tap $(( (X1 + X2) / 2 )) $(( (Y1 + Y2) / 2 ))
for i in $(seq 1 15); do  # wait until the keyboard has laid out its keys
  adb logcat -d -s BornomalaKeys:D | grep -q " key phonetic a " && break
  sleep 1
done

# tap LAYOUT KEY: look up the key centre logged by the keyboard.
tap() {
  local pos
  pos=$(adb logcat -d -s BornomalaKeys:D | grep " key $1 $2 " | tail -1 | awk '{print $(NF-1), $NF}')
  [ -n "$pos" ] || fail "key '$2' of layout '$1' not found in geometry log"
  adb shell input tap $pos
  sleep 0.5
}

SPACE="#-9"; LANG="#-7"

# Regression check: the keyboard must sit at the bottom of the screen, not the top.
SCREEN_H=$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1 | cut -dx -f2)
SPACE_Y=$(adb logcat -d -s BornomalaKeys:D | grep " key phonetic $SPACE " | tail -1 | awk '{print $NF}')
echo "screen height=$SCREEN_H space bar y=$SPACE_Y"
[ -n "$SPACE_Y" ] && [ "$SPACE_Y" -gt $(( SCREEN_H * 3 / 4 )) ] || fail "keyboard is not at the bottom of the screen"
tap phonetic a; tap phonetic m; tap phonetic i; tap phonetic "$SPACE"
sleep 1
T1=$(field_text); echo "after phonetic: $T1"
echo "$T1" | grep -q 'আমি' || fail "phonetic typing did not produce আমি"

tap phonetic "$LANG"; sleep 1
tap english h; tap english i
sleep 1
T2=$(field_text); echo "after english: $T2"
echo "$T2" | grep -q 'আমি hi' || fail "English typing after the language switch failed"

adb logcat -d > "$OUT/logcat.txt"
if grep -q "FATAL EXCEPTION" "$OUT/logcat.txt"; then fail "the app crashed during the smoke test"; fi
adb exec-out screencap -p > "$OUT/screen.png" || true
echo "Smoke test passed"
