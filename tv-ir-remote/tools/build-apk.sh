#!/usr/bin/env bash
# Builds a signed release APK without Gradle/AGP, using only command-line tools:
#   aapt (resources), kotlinc 1.9 + javac (code), ProGuard (shrinking), dx (dex),
#   zipalign + apksigner (packaging).
#
# Required environment (defaults match the CI container this was written in):
#   ANDROID_JAR   android.jar for API 34 (a Robolectric android-all jar also works)
#   KOTLIN_HOME   kotlinc 1.9.x distribution (its stdlib must not use invokedynamic)
#   PROGUARD_CP   classpath with proguard-base, proguard-core and their dependencies
#   DX            dx / dalvik-exchange binary
#   KEYSTORE      signing keystore (created automatically if missing)
set -euo pipefail

HERE="$(cd "$(dirname "$0")/.." && pwd)"
APP="$HERE/app/src/main"
OUT="$HERE/build"
ANDROID_JAR="${ANDROID_JAR:-/opt/android-sdk/platforms/android-34/android.jar}"
KOTLIN_HOME="${KOTLIN_HOME:-/tmp/claude-0/sdkdl/k19/kotlinc}"
PROGUARD_CP="${PROGUARD_CP:-$(ls /tmp/claude-0/sdkdl/pg/*.jar 2>/dev/null | tr '\n' ':')}"
DX="${DX:-dalvik-exchange}"
KEYSTORE="${KEYSTORE:-$HERE/build/release.jks}"
KS_PASS="${KS_PASS:-tvremote}"
APK_NAME="${APK_NAME:-TV-Remote-IR.apk}"
PACKAGE=com.irremote.tv
VERSION_CODE=1
VERSION_NAME=1.0
MIN_SDK=21
TARGET_SDK=34

rm -rf "$OUT/gen" "$OUT/classes" "$OUT/tmp"
mkdir -p "$OUT/gen" "$OUT/classes" "$OUT/tmp"

echo "==> Resources"
# The source manifest is kept Gradle/AGP-style (namespace and SDK levels live in
# app/build.gradle.kts), so add them here for aapt.
sed -e "s#<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">#<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" package=\"$PACKAGE\" android:versionCode=\"$VERSION_CODE\" android:versionName=\"$VERSION_NAME\">\n    <uses-sdk android:minSdkVersion=\"$MIN_SDK\" android:targetSdkVersion=\"$TARGET_SDK\" />#" \
  "$APP/AndroidManifest.xml" > "$OUT/tmp/AndroidManifest.xml"
aapt package -f -m --auto-add-overlay \
  -J "$OUT/gen" -M "$OUT/tmp/AndroidManifest.xml" -S "$APP/res" -A "$APP/assets" \
  -I "$ANDROID_JAR" -F "$OUT/tmp/resources.apk"

echo "==> Compile"
javac -nowarn -encoding UTF-8 --release 8 -cp "$ANDROID_JAR" -d "$OUT/classes" \
  $(find "$OUT/gen" -name '*.java') 2>&1 | grep -v '^Picked up' || true
"$KOTLIN_HOME/bin/kotlinc" $(find "$APP/java" -name '*.kt') \
  -cp "$ANDROID_JAR:$OUT/classes" -d "$OUT/classes" \
  -jvm-target 1.8 -Xlambdas=class -Xsam-conversions=class -Xno-param-assertions -Xno-call-assertions \
  -Xno-receiver-assertions -nowarn 2>&1 | grep -v '^Picked up' || true
test -f "$OUT/classes/com/irremote/tv/MainActivity.class" || { echo "Kotlin compilation failed"; exit 1; }

echo "==> Shrink"
cat > "$OUT/tmp/proguard.pro" <<EOF
-injars $OUT/classes
-injars $KOTLIN_HOME/lib/kotlin-stdlib.jar(!META-INF/**)
-outjars $OUT/tmp/app.jar
-libraryjars $ANDROID_JAR
-libraryjars <java.home>/jmods/java.base.jmod(!**.jar;!module-info.class)
-dontwarn
-dontnote
-dontobfuscate
-optimizationpasses 3
-optimizations !code/allocation/variable,!method/marking/static
-keep public class com.irremote.tv.MainActivity { *; }
-keepclassmembers enum * { public static **[] values(); public static ** valueOf(java.lang.String); }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
EOF
java -cp "$PROGUARD_CP" proguard.ProGuard @"$OUT/tmp/proguard.pro" 2>&1 | grep -v '^Picked up' | tail -2

echo "==> Dex"
"$DX" --dex --no-locals --min-sdk-version=21 --output="$OUT/tmp/classes.dex" "$OUT/tmp/app.jar" 2>&1 | grep -v '^Picked up' || true
test -f "$OUT/tmp/classes.dex" || { echo "dex failed"; exit 1; }

echo "==> Package"
cp "$OUT/tmp/resources.apk" "$OUT/tmp/unsigned.apk"
(cd "$OUT/tmp" && zip -q -9 unsigned.apk classes.dex)
zipalign -f -p 4 "$OUT/tmp/unsigned.apk" "$OUT/tmp/aligned.apk"
if [ ! -f "$KEYSTORE" ]; then
  keytool -genkeypair -keystore "$KEYSTORE" -storepass "$KS_PASS" -keypass "$KS_PASS" -alias tvremote \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=TV Remote IR" 2>&1 | grep -v '^Picked up' || true
fi
apksigner sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" --ks-key-alias tvremote \
  --out "$OUT/$APK_NAME" "$OUT/tmp/aligned.apk" 2>&1 | grep -v '^Picked up' || true
apksigner verify "$OUT/$APK_NAME" 2>&1 | grep -v '^Picked up' || true
ls -la "$OUT/$APK_NAME"
