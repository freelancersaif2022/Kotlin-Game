#!/usr/bin/env bash
# Builds a signed *debug* APK without the Android Gradle Plugin.
#
# Use this only when Google's Maven repository / SDK manager are unreachable (as in the CI
# container this project was developed in). With a normal Android SDK, use instead:
#     ./gradlew :android:assembleDebug
#
# Requirements: JDK 17+ (plus JDK 8 or 11 for Retrolambda), and the Android build tools aapt, dx/dalvik-exchange, zipalign,
# apksigner on PATH (Debian/Ubuntu: apt install aapt dalvik-exchange zipalign apksigner), plus an
# android.jar (API 34). Set ANDROID_JAR, or the script downloads the public android-34 jar.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$ROOT/build/offline-apk"
mkdir -p "$OUT"
ANDROID_JAR="${ANDROID_JAR:-$OUT/android-34.jar}"
if [ ! -s "$ANDROID_JAR" ]; then
  echo "Downloading android.jar (API 34)..."
  curl -fsSL -o "$ANDROID_JAR" https://raw.githubusercontent.com/Sable/android-platforms/master/android-34/android.jar
fi
APP_ID=$(grep '^appId=' "$ROOT/gradle.properties" | cut -d= -f2)
VERSION_NAME=$(grep '^appVersionName=' "$ROOT/gradle.properties" | cut -d= -f2)
VERSION_CODE=$(grep '^appVersionCode=' "$ROOT/gradle.properties" | cut -d= -f2)

echo "== 1/6 compile Kotlin (core + Android launcher) and collect runtime jars"
"$ROOT/gradlew" -p "$ROOT" :android-offline:offlineInputs -PofflineApk=true -PskipAndroid=true -PandroidJar="$ANDROID_JAR" -q
IN="$ROOT/android-offline/build/offline"

echo "== 2/6 strip Java 9+ metadata the dexer does not understand"
STRIP="$OUT/jars"; rm -rf "$STRIP"; mkdir -p "$STRIP"
for j in "$IN"/jars/*.jar; do
  case "$(basename "$j")" in annotations-*.jar) continue;; esac
  cp "$j" "$STRIP/"
  zip -q -d "$STRIP/$(basename "$j")" 'module-info.class' 'META-INF/versions/*' >/dev/null 2>&1 || true
done

echo "== 3/6 desugar for Android 5.0+ (Retrolambda) and dex"
# The Kotlin stdlib contains invokedynamic lambdas and the classic dexer only accepts them for
# API 26+. Retrolambda rewrites them (and default interface methods) into plain classes.
# It must run on JDK 8-11: set RETRO_JAVA or install openjdk-11-jdk-headless.
RETRO_JAVA="${RETRO_JAVA:-$(ls -d /usr/lib/jvm/java-11-openjdk*/bin/java /usr/lib/jvm/java-8-openjdk*/bin/java 2>/dev/null | head -1 || true)}"
[ -x "$RETRO_JAVA" ] || { echo "Need JDK 8 or 11 for Retrolambda (set RETRO_JAVA)"; exit 1; }
RETRO_JAR="$OUT/retrolambda-2.5.7.jar"
[ -s "$RETRO_JAR" ] || curl -fsSL -o "$RETRO_JAR" https://repo1.maven.org/maven2/net/orfjackal/retrolambda/retrolambda/2.5.7/retrolambda-2.5.7.jar
CLS="$OUT/classes-in"; DESUGARED="$OUT/classes-out"
rm -rf "$CLS" "$DESUGARED"; mkdir -p "$CLS" "$DESUGARED"
for j in "$STRIP"/*.jar; do (cd "$CLS" && unzip -qo "$j" -x 'META-INF/*' || true); done
"$RETRO_JAVA" -Dretrolambda.inputDir="$CLS" -Dretrolambda.outputDir="$DESUGARED" \
  -Dretrolambda.classpath="$CLS:$ANDROID_JAR" -Dretrolambda.defaultMethods=true -Dretrolambda.bytecodeVersion=51 \
  -javaagent:"$RETRO_JAR" -jar "$RETRO_JAR" > "$OUT/retrolambda.log" 2>&1 || { tail -20 "$OUT/retrolambda.log"; exit 1; }
DEX="$OUT/dex"; rm -rf "$DEX"; mkdir -p "$DEX"
DX="${DX:-$(command -v dx || command -v dalvik-exchange)}"
"$DX" --dex --multi-dex --min-sdk-version=21 --output="$DEX" "$DESUGARED"

echo "== 4/6 package resources, assets and manifest"
MANIFEST="$OUT/AndroidManifest.xml"
sed "s|<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">|<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"$APP_ID\">|" "$ROOT/android/AndroidManifest.xml" > "$MANIFEST"
UNSIGNED="$OUT/bramble-dash-unaligned.apk"
rm -f "$UNSIGNED"
aapt package -f -M "$MANIFEST" -S "$ROOT/android/res" -A "$ROOT/assets" -I "$ANDROID_JAR" -F "$UNSIGNED" \
  --min-sdk-version 21 --target-sdk-version 34 --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
  -0 ogg -0 wav -0 png
(cd "$DEX" && zip -q -r "$UNSIGNED" ./*.dex)
(cd "$IN" && zip -q -r "$UNSIGNED" lib)

echo "== 5/6 align"
ALIGNED="$OUT/bramble-dash-aligned.apk"
zipalign -f -p 4 "$UNSIGNED" "$ALIGNED"

echo "== 6/6 sign with a throwaway debug key (never committed)"
KS="$OUT/debug.keystore"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" >/dev/null 2>&1
fi
APK="$OUT/bramble-dash-debug.apk"
apksigner sign --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true --ks "$KS" --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey --out "$APK" "$ALIGNED"
apksigner verify --verbose "$APK" | head -5
aapt dump badging "$APK" | grep -E "package:|sdkVersion|targetSdkVersion|launchable-activity|native-code|application-label:"
ls -l "$APK"
echo "APK: $APK"
