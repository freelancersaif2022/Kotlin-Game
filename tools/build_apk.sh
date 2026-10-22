#!/usr/bin/env bash
# Builds the APK without the Android Gradle Plugin (used where Google Maven is
# unreachable). Normal builds should use:  ./gradlew assembleRelease
#
# Needs: ANDROID_JAR (platform android.jar), BUILD_TOOLS (dir with aapt2,
# zipalign, apksigner), KOTLIN_LIB (dir with kotlin-compiler-embeddable and its
# deps), dx (dalvik-exchange), proguard.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ANDROID_JAR="${ANDROID_JAR:-/root/android-sdk/platforms/android-34/android.jar}"
# java.* stubs (the API-34 jar is a Robolectric build without libcore)
CORE_JAR="${CORE_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}"
BUILD_TOOLS="${BUILD_TOOLS:-/usr/lib/android-sdk/build-tools/debian}"
KOTLIN_LIB="${KOTLIN_LIB:-/root/kt/lib}"
OUT="$ROOT/build/manual"
APP="$ROOT/app/src/main"
VERSION_CODE=1
VERSION_NAME=1.0

rm -rf "$OUT" && mkdir -p "$OUT"/{gen,classes,res}

echo "> resources"
"$BUILD_TOOLS/aapt2" compile --dir "$APP/res" -o "$OUT/res.zip"
sed 's|<manifest |<manifest package="com.smokemaster.game" |' "$APP/AndroidManifest.xml" > "$OUT/AndroidManifest.xml"
"$BUILD_TOOLS/aapt2" link -I "$ANDROID_JAR" --manifest "$OUT/AndroidManifest.xml" \
    --min-sdk-version 24 --target-sdk-version 34 \
    --version-code $VERSION_CODE --version-name $VERSION_NAME \
    -A "$APP/assets" -0 ogg --auto-add-overlay \
    --java "$OUT/gen" -o "$OUT/base.apk" "$OUT/res.zip"

echo "> kotlin"
KOTLINC_CP=$(ls "$KOTLIN_LIB"/*.jar | tr '\n' ':')
STDLIB=$(ls "$KOTLIN_LIB"/kotlin-stdlib-[0-9]*.jar)
java -Xmx2g -cp "$KOTLINC_CP" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -no-jdk -no-reflect -no-stdlib -classpath "$ANDROID_JAR:$CORE_JAR:$STDLIB" \
    -jvm-target 1.8 -Xlambdas=class -Xsam-conversions=class -Xno-param-assertions -Xno-call-assertions \
    -Xno-receiver-assertions -nowarn \
    -d "$OUT/classes" $(find "$APP/java" -name '*.kt')

echo "> shrink"
# ProGuard 6 cannot read the Java 17 API-34 jar, so it only sees the API-23
# library; newer framework references are left untouched (-dontwarn).
cat > "$OUT/proguard.pro" <<PRO
-injars $OUT/classes
-injars $STDLIB(!META-INF/**)
-outjars $OUT/min.jar
-libraryjars $CORE_JAR
-dontwarn
-dontnote
-dontobfuscate
-dontoptimize
-keep public class com.smokemaster.game.MainActivity { public <init>(); }
-keepclassmembers class * extends android.app.Activity { public void *(...); }
-keepclassmembers class * implements android.view.SurfaceHolder\$Callback { public void *(...); }
-keepclassmembers class * implements android.view.Choreographer\$FrameCallback { public void *(...); }
PRO
proguard @"$OUT/proguard.pro" > "$OUT/proguard.log" 2>&1 || { cat "$OUT/proguard.log"; exit 1; }

echo "> dex"
dalvik-exchange --dex --min-sdk-version=24 --output="$OUT/classes.dex" "$OUT/min.jar"

echo "> package"
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT" && zip -q -j unsigned.apk classes.dex)
"$BUILD_TOOLS/zipalign" -f -p 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

KEYSTORE="${KEYSTORE:-$ROOT/tools/smokemaster.keystore}"
if [ ! -f "$KEYSTORE" ]; then
    keytool -genkeypair -keystore "$KEYSTORE" -storepass smokemaster -keypass smokemaster \
        -alias smokemaster -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Smoke Master, O=Smoke Master, C=US" >/dev/null 2>&1
fi
"$BUILD_TOOLS/apksigner" sign --ks "$KEYSTORE" --ks-pass pass:smokemaster --key-pass pass:smokemaster \
    --ks-key-alias smokemaster --min-sdk-version 24 --out "$ROOT/build/SmokeMaster.apk" "$OUT/aligned.apk"
"$BUILD_TOOLS/apksigner" verify "$ROOT/build/SmokeMaster.apk"
ls -la "$ROOT/build/SmokeMaster.apk"
