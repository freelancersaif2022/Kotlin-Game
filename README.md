# CloneHub

A fast, ultra-light **app clone & shortcut manager** for Android, built with
**Kotlin + Jetpack Compose + Material 3**.

Pick any installed app, create a **clone** with a **custom name and accent
color**, and it stays on your CloneHub home grid **for life** (persisted
locally — survives reboots and app restarts). Rename or recolor a clone
anytime, tap to launch, or delete it.

## Features
- ⚡ Jetpack Compose UI, Material 3, dynamic color, dark/light, edge-to-edge
- ✨ Spring press animations, animated grid item placement, gradient accents
- 🏷️ Create clones, **rename** and **recolor** them, **delete** them
- ♾️ Clones are saved with DataStore and persist for the lifetime of the install
- 🪶 No database or serialization libraries, R8 + resource shrinking → tiny APK
- 🎨 Custom adaptive launcher icon (vector, with PNG fallback for Android 7.x)

## What "clone" means here
CloneHub creates renamable, re-styleable **launcher entries** that point at an
installed app and persist forever. It does **not** run two isolated OS-level
instances of an app at once — true app virtualization needs a sandbox engine
(VirtualApp/VirtualXposed-style) that is tens of thousands of lines, breaks on
modern Android, and cannot fit in a 2–3 MB APK. For a second **account**, use
the clone to launch the app, or Android's built-in Dual Apps / Work profile.

## Build
Requires JDK 17 and the Android SDK.

```bash
./gradlew :app:assembleRelease
# APK at app/build/outputs/apk/release/app-release.apk
```

## Releases
Every push builds the release APK in GitHub Actions and attaches it to a
GitHub Release. See the **Actions** tab and the **Releases** page.

## Tech
- minSdk 24 (Android 7.0), targetSdk 34
- Jetpack Compose (BOM 2024.09), Material 3, Lifecycle, DataStore Preferences
- R8 full mode + `shrinkResources` on release
